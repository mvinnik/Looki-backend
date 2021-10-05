package delegate

import ANDROID_DEV_CLIENT_ID
import AUTH_SECRET_KEY
import AuthProvider
import JWT_ISSUER
import SignInRequest
import SignInResponse
import User
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTCreationException
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.protobuf.util.JsonFormat
import com.mongodb.client.model.Filters
import com.mongodb.reactivestreams.client.MongoDatabase
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.bson.Document
import org.bson.types.ObjectId

class SignInDelegate(
    private val db: MongoDatabase,
    private val printer: JsonFormat.Printer,
    private val parser: JsonFormat.Parser,
) {

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun signIn(request: SignInRequest): SignInResponse =
        when (request.provider) {
            AuthProvider.AUTH_PROVIDER_GOOGLE -> {
                val verifier =
                    GoogleIdTokenVerifier.Builder(NetHttpTransport.Builder().build(), GsonFactory.getDefaultInstance())
                        .setAudience(mutableListOf(ANDROID_DEV_CLIENT_ID))
                        .build()

                val idToken = verifier.verify(request.token)
                if (idToken != null) {
                    val payload = idToken.payload

                    val usersCol = db.getCollection("users")
                    val filterByEmail = Filters.eq("email", payload.email)
                    var userDocument = usersCol.find(filterByEmail).awaitFirstOrNull()

                    val jwtToken = generateJwtToken()

                    if (userDocument != null) {
                        userDocument = userDocument.append("accessToken", jwtToken)
                        usersCol.findOneAndUpdate(filterByEmail, userDocument).awaitSingle()
                    } else {
                        userDocument = Document.parse(
                            printer.print(
                                User.newBuilder()
                                    .setId(ObjectId.get().toHexString())
                                    .setAccessToken(jwtToken)
                                    .setEmail(payload.email)
                                    .setName(payload["name"] as String)
                                    .setPhoto(payload["picture"] as String?)
                                    .build()
                            )
                        )
                        usersCol.insertOne(userDocument!!).awaitSingle()
                    }

                    val userBuilder = User.newBuilder()
                    parser.merge(userDocument!!.toJson(), userBuilder)
                    SignInResponse.newBuilder().setUser(userBuilder).build()
                } else {
                    throw IllegalArgumentException("Invalid token.")
                }
            }
            else -> throw IllegalArgumentException("Unknown AuthProvider: ${request.provider}.")
        }

    private fun generateJwtToken(): String =
        try {
            val algorithm = Algorithm.HMAC256(AUTH_SECRET_KEY)
            JWT.create()
                .withIssuer(JWT_ISSUER)
                .sign(algorithm)
        } catch (e: JWTCreationException) {
            println(e.localizedMessage)
            throw e
        }

}