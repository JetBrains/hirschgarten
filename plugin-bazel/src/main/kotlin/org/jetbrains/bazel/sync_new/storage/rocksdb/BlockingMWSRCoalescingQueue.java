package org.jetbrains.bazel.sync_new.storage.rocksdb;

import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class BlockingMWSRCoalescingQueue<V> {

  private static class Node<V> {
    public V value = null;
    public Node<V> next = null;
    public volatile boolean removed = true;
  }

  private static final VarHandle HEAD_HANDLE;
  private static final VarHandle TAIL_HANDLE;
  private static final VarHandle WAITER_HANDLE;
  private static final VarHandle NEXT_HANDLE;

  // increase for better bulk transfer performance
  private static final int SPIN_ATTEMPTS = 100;
  private static final int PARK_ATTEMPTS = 1000;

  static {
    try {
      MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(BlockingMWSRCoalescingQueue.class, MethodHandles.lookup());
      HEAD_HANDLE = lookup.findVarHandle(BlockingMWSRCoalescingQueue.class, "head", Node.class);
      TAIL_HANDLE = lookup.findVarHandle(BlockingMWSRCoalescingQueue.class, "tail", Node.class);
      WAITER_HANDLE = lookup.findVarHandle(BlockingMWSRCoalescingQueue.class, "waiter", Thread.class);
      NEXT_HANDLE = lookup.findVarHandle(Node.class, "next", Node.class);
    }
    catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private ConcurrentMap<V, Node<V>> lastNodes;
  private Node<V> head;
  private Node<V> tail;
  private Thread waiter;

  public BlockingMWSRCoalescingQueue() {
    this.lastNodes = new ConcurrentHashMap<>();
    Node<V> sentinel = new Node<>();
    sentinel.removed = true;
    this.head = sentinel;
    this.tail = sentinel;
  }

  public V poll() {
    while (true) {
      Node<V> head = (Node<V>)HEAD_HANDLE.getAcquire(this);
      Node<V> next = (Node<V>)NEXT_HANDLE.getAcquire(head);

      if (next == null) {
        return null;
      }

      boolean isCoalescingSupported = this.isCoalescingSupported(next.value);
      if (isCoalescingSupported && next.removed) {
        if (HEAD_HANDLE.compareAndSet(this, head, next)) {
          next.value = null;
        }
        continue;
      }

      if (HEAD_HANDLE.compareAndSet(this, head, next)) {
        V value = next.value;
        next.value = null;

        if (isCoalescingSupported) {
          lastNodes.remove(value);
        }
        return value;
      }
    }
  }

  public V take(long time, TimeUnit unit) throws InterruptedException {
    long started = System.nanoTime();
    long timeout = unit.toNanos(time);
    int failures = 1;
    while (true) {
      if (Thread.interrupted()) {
        throw new InterruptedException();
      }

      V value = this.poll();
      if (value != null) {
        return value;
      }

      if (System.nanoTime() - started > timeout) {
        return null;
      }

      WAITER_HANDLE.setVolatile(this, Thread.currentThread());

      VarHandle.fullFence();

      value = this.poll();
      if (value != null) {
        WAITER_HANDLE.compareAndSet(this, Thread.currentThread(), null);
        return value;
      }

      failures = this.spin(failures);

      if (failures < PARK_ATTEMPTS) {
        WAITER_HANDLE.compareAndSet(this, Thread.currentThread(), null);
      }
    }
  }

  public void offer(V value) {
    Node<V> old = lastNodes.remove(value);
    if (old != null) {
      old.removed = true;
    }

    Node<V> newNode = new Node<>();
    newNode.value = value;
    newNode.removed = false;

    Node<V> prevTail = (Node<V>)TAIL_HANDLE.getAndSet(this, newNode);
    NEXT_HANDLE.setRelease(prevTail, newNode);

    if (this.isCoalescingSupported(value)) {
      lastNodes.put(value, newNode);
    }

    Thread waiter = (Thread)WAITER_HANDLE.getAndSet(this, null);
    if (waiter != null) {
      LockSupport.unpark(waiter);
    }
  }

  public void drainTo(List<V> list, int maxElements) {
    Node<V> head = (Node<V>)HEAD_HANDLE.getVolatile(this);
    Node<V> node = (Node<V>)NEXT_HANDLE.getVolatile(head);
    Node<V> lastConsumed = head;

    while (node != null && maxElements > 0) {
      V value = node.value;
      boolean coalesce = this.isCoalescingSupported(value);

      if (!coalesce || !node.removed) {
        if (value != null) {
          list.add(value);
          maxElements--;
          if (coalesce) {
            lastNodes.remove(value);
          }
        }
      }
      node.value = null;
      lastConsumed = node;
      node = (Node<V>)NEXT_HANDLE.getVolatile(node);
    }

    if (lastConsumed != head) {
      HEAD_HANDLE.compareAndSet(this, head, lastConsumed);
    }
  }

  public boolean isEmpty() {
    return head.next == null;
  }

  protected boolean isCoalescingSupported(@Nullable V value) {
    return true;
  }

  private int spin(int failures) {
    if (failures < SPIN_ATTEMPTS) {
      for (int i = 0; i < failures; i++) {
        Thread.onSpinWait();
      }
      failures <<= 1;
    }
    else if (failures < PARK_ATTEMPTS) {
      Thread.yield();
      LockSupport.parkNanos(10_000L);
      failures += 1;
    }
    else {
      Thread.yield();
      LockSupport.parkNanos(10_000L * failures);
    }
    return failures;
  }
}
