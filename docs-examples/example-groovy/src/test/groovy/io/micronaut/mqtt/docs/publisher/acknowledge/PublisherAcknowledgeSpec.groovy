package io.micronaut.mqtt.docs.publisher.acknowledge

import io.micronaut.context.ApplicationContext
import io.micronaut.mqtt.test.AbstractMQTTTest
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import spock.util.concurrent.PollingConditions

import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

class PublisherAcknowledgeSpec extends AbstractMQTTTest {

    void "test publisher acknowledgement"() {
        given:
        ApplicationContext applicationContext = startContext()
        PollingConditions conditions = new PollingConditions(timeout: 5)
        AtomicInteger successCount = new AtomicInteger(0)
        AtomicInteger errorCount = new AtomicInteger(0)

        when:
        ProductClient productClient = applicationContext.getBean(ProductClient)
        Publisher<Void> publisher = productClient.sendPublisher("publisher body".bytes)
        CompletableFuture<Void> future = productClient.sendFuture("future body".bytes)
        ProductListener listener = applicationContext.getBean(ProductListener.class)

        Subscriber<Void> subscriber = new Subscriber<Void>() {
            @Override
            void onSubscribe(Subscription subscription) { }

            @Override
            void onNext(Void aVoid) {
                throw new UnsupportedOperationException("Should never be called")
            }

            @Override
            void onError(Throwable throwable) {
                // if an error occurs
                errorCount.incrementAndGet()
            }

            @Override
            void onComplete() {
                // if the publish was acknowledged
                successCount.incrementAndGet()
            }
        }
        publisher.subscribe(subscriber)
        future.whenComplete {v, t ->
            if (t == null) {
                successCount.incrementAndGet()
            } else {
                errorCount.incrementAndGet()
            }
        }

        then:
        conditions.eventually {
            errorCount.get() == 0
            successCount.get() == 2
            listener.messageLengths.size() == 2
        }

        cleanup:
        applicationContext.close()
    }

}
