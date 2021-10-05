import com.mongodb.client.model.Filters
import com.mongodb.reactivestreams.client.MongoClients
import com.mongodb.reactivestreams.client.MongoDatabase
import io.grpc.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitFirstOrNull

fun main() {
//    val port = System.getenv("PORT")?.toInt() ?: 50051
    val server = HelloWorldServer(8980)
    server.start()
    server.blockUntilShutdown()
}

class HelloWorldServer(private val port: Int) {
    private val mongoClient = MongoClients.create()
    private val db = mongoClient.getDatabase("looki-dev")

    private val server = ServerBuilder
        .forPort(port)
        .addService(LookiService(db))
        .intercept(AuthInterceptor(db))
        .build()

    fun start() {
        server.start()
        println("Server started, listening on $port")
        Runtime.getRuntime().addShutdownHook(
            Thread {
                println("*** shutting down gRPC server since JVM is shutting down")
                this@HelloWorldServer.stop()
                println("*** server shut down")
            }
        )
    }

    private fun stop() {
        server.shutdown()
    }

    fun blockUntilShutdown() {
        server.awaitTermination()
    }
}

class AuthInterceptor(private val db: MongoDatabase) : ServerInterceptor {

    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        when (call.methodDescriptor.bareMethodName) {
            "SignIn" -> {
                val token = headers[Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER)]
                    ?.substring(0, AUTH_TYPE_BEARER.length)
                    ?.trim()

                if (token == null) call.close(Status.UNAUTHENTICATED, headers)

                val filterById = Filters.eq("access_token", token)
                CoroutineScope(Dispatchers.IO).launch {
                    val user = db.getCollection("users").find(filterById).awaitFirstOrNull()
                    if (user == null) {
                        call.close(Status.UNAUTHENTICATED, headers)
                    } else {
                        next.startCall(call, headers)
                    }
                }
            }
        }

        return object : ServerCall.Listener<ReqT>() {}
    }

}