package ome.smuggler.q;

import static java.util.Objects.requireNonNull;
import static ome.smuggler.q.MessageBody.writeBody;

import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientProducer;

import ome.smuggler.core.msg.ChannelSource;

/**
 * Puts messages on a queue, asynchronously.
 * Messages are durable by default but any other kind of message can be
 * constructed by overriding the {@link #newMessage(QueueConnector) newMessage}
 * method. 
 */
public class EnqueueTask<T> implements ChannelSource<T> {

    private final QueueConnector queue;
    private final ClientProducer producer;
    
    /**
     * Creates a new instance.
     * @param queue provides access to the queue on which to put messages. 
     * @throws HornetQException if a queue producer could not be created.
     * @throws NullPointerException if any argument is {@code null}.
     */
    public EnqueueTask(QueueConnector queue) throws HornetQException {
        requireNonNull(queue, "queue");
        
        this.queue = queue;
        this.producer = queue.newProducer();
    }
    
    protected ClientMessage newMessage(QueueConnector queue) {
        return queue.newDurableMessage();
    }
    
    @Override
    public void send(T data) throws Exception {
        ClientMessage msg = newMessage(queue);
        writeBody(msg, data);
        producer.send(msg);
    }

}
