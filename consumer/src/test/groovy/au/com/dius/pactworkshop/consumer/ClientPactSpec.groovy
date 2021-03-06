package au.com.dius.pactworkshop.consumer

import au.com.dius.pact.consumer.PactError
import au.com.dius.pact.consumer.PactError$
import au.com.dius.pact.consumer.PactVerified$
import au.com.dius.pact.consumer.VerificationResult
import au.com.dius.pact.consumer.groovy.PactBuilder
import spock.lang.Specification

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class ClientPactSpec extends Specification {

  private Client client
  private LocalDateTime date
  private PactBuilder provider

  def setup() {
    client = new Client('http://localhost:1234')
    date = LocalDateTime.now()
    provider = new PactBuilder()
    provider {
      serviceConsumer 'Our Little Consumer'
      hasPactWith 'Our Provider'
      port 1234
    }
  }

  def 'Pact with our provider'() {
    given:
    def json = [
      test: 'NO',
      date: '2013-08-16T15:31:20+10:00',
      count: 100
    ]
    provider {
      given('data count > 0')
      uponReceiving('a request for json data')
      withAttributes(path: '/provider.json', query: [validDate: date.toString()])
      willRespondWith(status: 200)
      withBody {
        test 'NO'
        validDate timestamp("yyyy-MM-dd'T'HH:mm:ssXXX", json.date)
        count integer(json.count)
      }
    }

    when:
    def result
    VerificationResult pactResult = provider.run {
      result = client.fetchAndProcessData(date.toString())
    }

    then:
    pactResult == PactVerified$.MODULE$
    result == [1, OffsetDateTime.parse(json.date, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx"))]
  }

  def 'handles a missing date parameter'() {
    given:
    provider {
      given('data count > 0')
      uponReceiving('a request with a missing date parameter')
      withAttributes(path: '/provider.json')
      willRespondWith(status: 400, body: '"validDate is required"', headers: ['Content-Type': 'application/json'])
    }

    when:
    VerificationResult pactResult = provider.run {
      client.fetchAndProcessData(null)
    }

    then:
    pactResult == PactVerified$.MODULE$
  }

  def 'handles an invalid date parameter'() {
    given:
    provider {
      given('data count > 0')
      uponReceiving('a request with an invalid date parameter')
      withAttributes(path: '/provider.json', query: [validDate: 'This is not a date'])
      willRespondWith(status: 400, body: $/"'This is not a date' is not a date"/$, headers: ['Content-Type': 'application/json'])
    }

    when:
    def result
    VerificationResult pactResult = provider.run {
      result = client.fetchAndProcessData('This is not a date')
    }

    then:
    pactResult == PactVerified$.MODULE$
  }

  def 'when there is no data'() {
    given:
    provider {
      given('data count == 0')
      uponReceiving('a request for json data')
      withAttributes(path: '/provider.json', query: [validDate: date.toString()])
      willRespondWith(status: 404)
    }

    when:
    def result
    VerificationResult pactResult = provider.run {
      result = client.fetchAndProcessData(date.toString())
    }

    then:
    pactResult == PactVerified$.MODULE$
  }

}
