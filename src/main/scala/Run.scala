import java.time.{LocalDateTime, ZoneId}

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.google.common.base.Supplier
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.http.index.IndexExistsResponse
import com.sksamuel.elastic4s.jackson.ElasticJackson.Implicits._
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.http.HttpHost
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback
import org.slf4j.LoggerFactory
import vc.inreach.aws.request.{AWSSigner, AWSSigningRequestInterceptor}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}


object Run extends App {
  private val log = LoggerFactory.getLogger(this.getClass.getCanonicalName.replace("$", ""))
  private val config = ConfigFactory.load()
  private val esConfig = config.getConfig("elasticsearch")
  private val esClient = createEsHttpClient(esConfig)

  esClient.execute(indexExists("some-index")).onComplete {
    case Success(indexResponse : IndexExistsResponse) if indexResponse.isExists =>
       log.info(s"Index exists")
    case Success(indexResponse : IndexExistsResponse) if !indexResponse.isExists =>
      log.info(s"No such index")
    case Failure(e) =>
      log.error(s"Failed to get index", e)
      throw e
  }

  esClient.close()

  private def createAwsSigner(config: Config): AWSSigner = {
    import com.gilt.gfc.guava.GuavaConversions._

    val awsCredentialsProvider = new DefaultAWSCredentialsProviderChain
    val service = config.getString("service")
    val region = config.getString("region")
    val clock: Supplier[LocalDateTime] = () => LocalDateTime.now(ZoneId.of("UTC"))
    new AWSSigner(awsCredentialsProvider, region, service, clock)
  }

  private def createEsHttpClient(config: Config): HttpClient = {
    val hosts = ElasticsearchClientUri(config.getString("uri")).hosts.map {
      case (host, port) =>
        new HttpHost(host, port, "http")
    }

    log.info(s"Creating HTTP client on ${hosts.mkString(",")}")

    val client = RestClient.builder(hosts: _*)
      .setHttpClientConfigCallback(new AWSSignerInteceptor)
      .build()
    HttpClient.fromRestClient(client)
  }

  private class AWSSignerInteceptor extends HttpClientConfigCallback {
    override def customizeHttpClient(httpClientBuilder: HttpAsyncClientBuilder): HttpAsyncClientBuilder = {
      httpClientBuilder.addInterceptorLast(new AWSSigningRequestInterceptor(createAwsSigner(esConfig)))
    }
  }
}
