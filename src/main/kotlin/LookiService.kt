import com.google.protobuf.util.JsonFormat
import com.mongodb.reactivestreams.client.MongoDatabase
import delegate.SignInDelegate

class LookiService(db: MongoDatabase) : LookiGrpcKt.LookiCoroutineImplBase() {

    private val printer = JsonFormat.printer().includingDefaultValueFields()
    private val parser = JsonFormat.parser()


    private val signInDelegate = SignInDelegate(db, printer, parser)


    override suspend fun signIn(request: SignInRequest): SignInResponse = signInDelegate.signIn(request)
}