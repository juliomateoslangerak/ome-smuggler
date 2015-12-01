package ome.smuggler.core.msg;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static util.object.Pair.pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

import util.object.Pair;

/**
 * A channel source that queues messages in memory and allows to retrieve the
 * head of the queue to consume messages.
 */
public class SourceQueue<M, D> implements ConfigurableChannelSource<M, D> {

    private final ConcurrentLinkedQueue<Pair<Optional<M>, D>> sendBuffer;
    
    /**
     * Creates a new instance.
     */
    public SourceQueue() {
        sendBuffer = new ConcurrentLinkedQueue<>();
    }
    
    private void send(Optional<M> metadata, D data) {
        requireNonNull(data, "data");
        sendBuffer.offer(pair(metadata, data));
    }
    
    @Override
    public void send(D data) throws Exception {
        send(Optional.empty(), data);
    }

    @Override
    public void send(M metadata, D data) throws Exception {
        requireNonNull(metadata, "metadata");
        send(Optional.of(metadata), data);
    }

    /**
     * @return the oldest message item (metadata + data) on the queue or empty 
     * if the queue has no items.
     */
    public Optional<Pair<Optional<M>, D>> head() {
        return Optional.ofNullable(sendBuffer.poll());
    }
    
    /**
     * Same as {@link #head()} but discards message metadata.
     * @return the data of the oldest message item on the queue or empty if the
     * queue has no items.
     */
    public Optional<D> headData() {
        return head().map(Pair::snd);
    }
    
    /**
     * Removes all the items currently in the queue, returning them in FIFO
     * order.
     * @return the items in the queue or empty if the queue has no items.
     */
    public List<Pair<Optional<M>, D>> dequeue() {
        ArrayList<Pair<Optional<M>, D>> queued = new ArrayList<>();
        while (!sendBuffer.isEmpty()) {
            Pair<Optional<M>, D> head = sendBuffer.poll();
            if (head != null) {   // (*)
                queued.add(head);
            } 
        }
        return queued;
    }
    /* NOTE. Race conditions.
     * Another thread may call head() and remove the last element, then we'd 
     * poll() and get null, which we don't want to add to queued.
     */
    
    /**
     * Same as {@link #dequeue()} but discards message metadata.
     * @return the data in each queued message or empty if the queue has no 
     * items.
     */
    public List<D> dequeueData() {
        return dequeue().stream().map(Pair::snd).collect(toList());
    }
    
}
