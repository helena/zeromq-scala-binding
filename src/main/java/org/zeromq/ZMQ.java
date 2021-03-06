/*
 * Copyright 2011 - 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zeromq;

import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.LongByReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * Offers an API similar to that of jzmq [1] written by Gonzalo Diethelm.
 * <p/>
 * 1. https://github.com/zeromq/jzmq
 */
public class ZMQ {
  private static final ZeroMQLibrary zmq = ZeroMQ$.MODULE$.loadLibrary();
  private static final int[] majorVersion = new int[1];
  private static final int[] minorVersion = new int[1];
  private static final int[] patchVersion = new int[1];

  public static final int NOBLOCK   = ZeroMQ$.MODULE$.ZMQ_NOBLOCK();
  public static final int DONTWAIT  = ZeroMQ$.MODULE$.ZMQ_DONTWAIT();
  public static final int PAIR      = ZeroMQ$.MODULE$.ZMQ_PAIR();
  public static final int SNDMORE   = ZeroMQ$.MODULE$.ZMQ_SNDMORE();
  public static final int PUB       = ZeroMQ$.MODULE$.ZMQ_PUB();
  public static final int SUB       = ZeroMQ$.MODULE$.ZMQ_SUB();
  public static final int REQ       = ZeroMQ$.MODULE$.ZMQ_REQ();
  public static final int REP       = ZeroMQ$.MODULE$.ZMQ_REP();
  public static final int XREQ      = ZeroMQ$.MODULE$.ZMQ_DEALER();
  public static final int XREP      = ZeroMQ$.MODULE$.ZMQ_ROUTER();
  public static final int DEALER    = ZeroMQ$.MODULE$.ZMQ_DEALER();
  public static final int ROUTER    = ZeroMQ$.MODULE$.ZMQ_ROUTER();
  public static final int PULL      = ZeroMQ$.MODULE$.ZMQ_PULL();
  public static final int PUSH      = ZeroMQ$.MODULE$.ZMQ_PUSH();
  public static final int STREAMER  = ZeroMQ$.MODULE$.ZMQ_STREAMER();
  public static final int FORWARDER = ZeroMQ$.MODULE$.ZMQ_FORWARDER();
  public static final int QUEUE     = ZeroMQ$.MODULE$.ZMQ_QUEUE();

  static {
    zmq.zmq_version(majorVersion, minorVersion, patchVersion);
  }

  public static byte[] intToByteArray(int value) {
    return new byte[]{(byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value};
  }

  public static int getMajorVersion() {
    return majorVersion[0];
  }

  public static int getMinorVersion() {
    return minorVersion[0];
  }

  public static int getPatchVersion() {
    return patchVersion[0];
  }

  public static int getFullVersion() {
    return makeVersion(getMajorVersion(), getMinorVersion(), getPatchVersion());
  }

  public static int makeVersion(int major, int minor, int patch) {
    return major * 10000 + minor * 100 + patch;
  }

  public static String getVersionString() {
    return String.format("%d.%d.%d", getMajorVersion(), getMinorVersion(), getPatchVersion());
  }

  public static Context context(int ioThreads) {
    return new Context(ioThreads);
  }

  /**
   * Creates a new Context with the default thread count of 1.
   */
  public static Context context() {
    return new Context();
  }

  /**
   * Represents a ZeroMQ Context
   */
  public static class Context {
    protected Pointer ptr;


    protected Context(int ioThreads) {
      ptr = (getFullVersion() < makeVersion(3, 2, 0)) ? zmq.zmq_init(ioThreads) : zmq.zmq_ctx_new();
    }

    /**
     * Creates a new Context with the default number of threads.
     */
    protected Context() {
      ptr = (getFullVersion() < makeVersion(3, 2, 0)) ? zmq.zmq_init(1) : zmq.zmq_ctx_new();
    }

    public Socket socket(int type) {
      return new Socket(this, type);
    }

    public Poller poller() {
      return new Poller(this);
    }

    public Poller poller(int size) {
      return new Poller(this, size);
    }

    /**
     * Returns zero if successful, otherwise returns -1.
     */
    public int destroy() {
      return (getFullVersion() < makeVersion(3, 2, 0)) ? zmq.zmq_term(ptr) : zmq.zmq_ctx_destroy(ptr);
    }
  }

  public static class Socket {
    protected Pointer ptr;
    MessageDataBuffer messageDataBuffer = new MessageDataBuffer();

    public void close() {
      zmq.zmq_close(ptr);
    }

    public int getType() {
      return (getFullVersion() < makeVersion(2, 1, 0)) ? -1 : (int) getLongSockopt(ZeroMQ$.MODULE$.ZMQ_TYPE());
    }

    public void setLinger(long linger) {
      if (getFullVersion() >= makeVersion(2, 1, 0)) setLongSockopt(ZeroMQ$.MODULE$.ZMQ_LINGER(), linger);
    }

    public long getLinger() {
      return (getFullVersion() >= makeVersion(2, 1, 0)) ? (int) getLongSockopt(ZeroMQ$.MODULE$.ZMQ_LINGER()) : -1;
    }

    public void setBacklog(long backlog) {
      if (getFullVersion() >= makeVersion(3, 0, 0)) setLongSockopt(ZeroMQ$.MODULE$.ZMQ_BACKLOG(), backlog);
    }

    public long getBacklog() {
      return (getFullVersion() >= makeVersion(3, 0, 0)) ? (int) getLongSockopt(ZeroMQ$.MODULE$.ZMQ_BACKLOG()) : -1;
    }

    public void setReconnectIVL(long reconnectIVL) {
      if (getFullVersion() >= makeVersion(3, 0, 0)) setLongSockopt(ZeroMQ$.MODULE$.ZMQ_RECONNECT_IVL(), reconnectIVL);
    }

    public long getReconnectIVL() {
      return (getFullVersion() >= makeVersion(3, 0, 0)) ? (int) getLongSockopt(ZeroMQ$.MODULE$.ZMQ_RECONNECT_IVL()) : -1;
    }

    public void setReconnectIVLMax(long reconnectIVLMax) {
      if (getFullVersion() >= makeVersion(3, 0, 0)) setLongSockopt(ZeroMQ$.MODULE$.ZMQ_RECONNECT_IVL_MAX(), reconnectIVLMax);
    }

    public long getReconnectIVLMax() {
      return (getFullVersion() >= makeVersion(3, 0, 0)) ? getLongSockopt(ZeroMQ$.MODULE$.ZMQ_RECONNECT_IVL_MAX()) : -1;
    }

    public void setMaxMsgSize(long maxMsgSize) {
      if (getFullVersion() >= makeVersion(3, 0, 0)) setLongSockopt(ZeroMQ$.MODULE$.ZMQ_MAXMSGSIZE(), maxMsgSize);
    }

    public long getMaxMsgSize() {
      return (getFullVersion() >= makeVersion(3, 0, 0)) ? getLongSockopt(ZeroMQ$.MODULE$.ZMQ_MAXMSGSIZE()) : -1;
    }

    public void setHWM(long hwm) {
      if (getFullVersion() < makeVersion(3, 0, 0)) setLongSockopt(ZeroMQ$.MODULE$.ZMQ_HWM(), hwm);
      else {
        setSndHWM(hwm);
        setRcvHWM(hwm);
      }
    }

    public long getHWM() {
      return (getFullVersion() < makeVersion(3, 0, 0)) ? getLongSockopt(ZeroMQ$.MODULE$.ZMQ_HWM()) : -1;
    }

    public void setSndHWM(long sndHWM) {
      if (getFullVersion() >= makeVersion(3, 0, 0)) setLongSockopt(ZeroMQ$.MODULE$.ZMQ_SNDHWM(), sndHWM);
    }

    public long getSndHWM() {
      return (getFullVersion() >= makeVersion(3, 0, 0)) ? getLongSockopt(ZeroMQ$.MODULE$.ZMQ_SNDHWM()) : -1;
    }

    public void setRcvHWM(long rcvHWM) {
      if (getFullVersion() >= makeVersion(3, 0, 0)) setLongSockopt(ZeroMQ$.MODULE$.ZMQ_RCVHWM(), rcvHWM);
    }

    public long getRcvHWM() {
      return (getFullVersion() >= makeVersion(3, 0, 0)) ? getLongSockopt(ZeroMQ$.MODULE$.ZMQ_RCVHWM()) : -1;
    }


    public void setSwap(long swap) {
      if (getFullVersion() < makeVersion(3, 0, 0)) setLongSockopt(ZeroMQ$.MODULE$.ZMQ_SWAP(), swap);
    }

    public long getSwap() {
      return (getFullVersion() < makeVersion(3, 0, 0)) ? getLongSockopt(ZeroMQ$.MODULE$.ZMQ_SWAP()) : -1;
    }

    public void setAffinity(long affinity) {
      setLongSockopt(ZeroMQ$.MODULE$.ZMQ_AFFINITY(), affinity);
    }

    public long getAffinity() {
      return getLongSockopt(ZeroMQ$.MODULE$.ZMQ_AFFINITY());
    }

    public void setIdentity(byte[] identity) {
      setBytesSockopt(ZeroMQ$.MODULE$.ZMQ_IDENTITY(), identity);
    }

    public byte[] getIdentity() {
      return getBytesSockopt(ZeroMQ$.MODULE$.ZMQ_IDENTITY());
    }

    public void setRate (long rate) {
      setLongSockopt(ZeroMQ$.MODULE$.ZMQ_RATE(), rate);
    }

    public long getRate() {
      return getLongSockopt(ZeroMQ$.MODULE$.ZMQ_RATE());
    }

    public void setRecoveryInterval(long recovery_ivl) {
      setLongSockopt(ZeroMQ$.MODULE$.ZMQ_RECONNECT_IVL(), recovery_ivl);
    }

    public long getRecoveryInterval() {
      return getLongSockopt(ZeroMQ$.MODULE$.ZMQ_RECOVERY_IVL());
    }

    public void setMulticastHops(long mcast_hops) {
      if (getFullVersion() >= makeVersion(3, 0, 0)) setLongSockopt(ZeroMQ$.MODULE$.ZMQ_MCAST_LOOP(), mcast_hops);
    }

    public long getMulticastHops() {
      return (getFullVersion() < makeVersion(3, 0, 0)) ? -1 : getLongSockopt(ZeroMQ$.MODULE$.ZMQ_MCAST_LOOP());
    }

    public void setReceiveTimeOut(long timeout) {
      if (getFullVersion() >= makeVersion(2, 2, 0)) setLongSockopt(ZeroMQ$.MODULE$.ZMQ_RCVTIMEO(), timeout);
    }

    public long getReceiveTimeOut() {
      return (getFullVersion() < makeVersion(2, 2, 0)) ? -1 : getLongSockopt(ZeroMQ$.MODULE$.ZMQ_RCVTIMEO());
    }

    public void setSendTimeOut(long timeout) {
      if (getFullVersion() >= makeVersion(2, 2, 0)) setLongSockopt(ZeroMQ$.MODULE$.ZMQ_SNDTIMEO(), timeout);
    }

    public long getSendTimeOut() {
      return (getFullVersion() < makeVersion(2, 2, 0)) ? -1 : getLongSockopt(ZeroMQ$.MODULE$.ZMQ_SNDTIMEO());
    }

    public void setSendBufferSize(long sndbuf) {
      setLongSockopt(ZeroMQ$.MODULE$.ZMQ_SNDBUF(), sndbuf);
    }

    public long getSendBufferSize() {
      return getLongSockopt(ZeroMQ$.MODULE$.ZMQ_SNDBUF());
    }

    public void setReceiveBufferSize(long rcvbuf) {
      setLongSockopt(ZeroMQ$.MODULE$.ZMQ_RCVBUF(), rcvbuf);
    }

    public long getReceiveBufferSize() {
      return getLongSockopt(ZeroMQ$.MODULE$.ZMQ_RCVBUF());
    }

    public long getFD() {
      return (getFullVersion() < makeVersion(2, 1, 0)) ? -1 : getLongSockopt(ZeroMQ$.MODULE$.ZMQ_FD());
    }

    public long getEvents() {
      return (getFullVersion() < makeVersion(2, 1, 0)) ? -1 : getLongSockopt(ZeroMQ$.MODULE$.ZMQ_EVENTS());
    }

    public void setMulticastLoop(boolean mcast_loop) {
      if (getFullVersion() < makeVersion(3, 0, 0)) setLongSockopt(ZeroMQ$.MODULE$.ZMQ_MCAST_LOOP(), mcast_loop ? 1 : 0);
    }

    public boolean hasMulticastLoop() {
      return getFullVersion() >= makeVersion(3, 0, 0) && getLongSockopt(ZeroMQ$.MODULE$.ZMQ_MCAST_LOOP()) != 0;
    }

    public boolean hasReceiveMore() {
      return getLongSockopt(ZeroMQ$.MODULE$.ZMQ_RCVMORE()) != 0;
    }

    public int subscribe(byte[] topic) {
      return setBytesSockopt(ZeroMQ$.MODULE$.ZMQ_SUBSCRIBE(), topic);
    }

    public int unsubscribe(byte[] topic) {
      return setBytesSockopt(ZeroMQ$.MODULE$.ZMQ_UNSUBSCRIBE(), topic);
    }

    public int bind(String addr) {
        return zmq.zmq_bind(ptr, addr);
    }

    public int connect(String addr) {
        return zmq.zmq_connect(ptr, addr);
    }

    public boolean send(byte[] msg, int flags) {
      zmq_msg_t message = newZmqMessage(msg);

      if ((getFullVersion() == makeVersion(3, 2, 0) && zmq.zmq_msg_send(message, ptr, flags) == -1) ||
          (getFullVersion() < makeVersion(3, 2, 0) && zmq.zmq_send(ptr, message, flags) != 0)) {
        if (zmq.zmq_errno() == ZeroMQ$.MODULE$.EAGAIN()) {
          if (zmq.zmq_msg_close(message) != 0) raiseZMQException();
          else return false;
        } else {
          zmq.zmq_msg_close(message);
          raiseZMQException();
          return false;
        }
      }

      if (zmq.zmq_msg_close(message) != 0) raiseZMQException();
      return true;
    }

    public byte[] recv(int flags) {
      zmq_msg_t message = newZmqMessage();

      if ((getFullVersion() == makeVersion(3, 2, 0) && zmq.zmq_msg_recv(message, ptr, flags) == -1) ||
          (getFullVersion() < makeVersion(3, 2, 0) && zmq.zmq_recv(ptr, message, flags) != 0)) {
        if (zmq.zmq_errno() == ZeroMQ$.MODULE$.EAGAIN()) {
          if (zmq.zmq_msg_close(message) != 0) raiseZMQException();
          else return null;
        } else {
          zmq.zmq_msg_close(message);
          raiseZMQException();
        }
      }

      Pointer data = zmq.zmq_msg_data(message);
      int length = zmq.zmq_msg_size(message);
      byte[] dataByteArray = data.getByteArray(0, length);

      if (zmq.zmq_msg_close(message) != 0) raiseZMQException();
      return dataByteArray;
    }

    protected Socket(Context context, int type) {
      ptr = zmq.zmq_socket(context.ptr, type);
    }

    @Override
    protected void finalize() {
      close();
    }

    private long getLongSockopt(int option) {
      Memory value = new Memory(Long.SIZE / 8);
      zmq.zmq_getsockopt(ptr, option, value, new LongByReference(Long.SIZE / 8));
      return value.getLong(0);
    }

    private int setLongSockopt(int option, long optval) {
      Memory value = new Memory(Long.SIZE / 8);
      value.setLong(0, optval);
      return zmq.zmq_setsockopt(ptr, option, value, new NativeLong(Long.SIZE / 8));
    }

    private byte[] getBytesSockopt(int option) {
      Memory value = new Memory(1024);
      LongByReference length = new LongByReference(1024);
      zmq.zmq_getsockopt(ptr, option, value, length);
      return value.getByteArray(0, (int) length.getValue());
    }

    private int setBytesSockopt(int option, byte[] optval) {
      Pointer value;
      if (optval.length > 0) {
        value = new Memory(optval.length);
        value.write(0, optval, 0, optval.length);
      } else value = Pointer.NULL;

      return zmq.zmq_setsockopt(ptr, option, value, new NativeLong(optval.length));
    }

    private zmq_msg_t newZmqMessage(byte[] msg) {
      zmq_msg_t message = new zmq_msg_t();
      if (msg.length == 0) {
        if (zmq.zmq_msg_init_size(message, new NativeLong(msg.length)) != 0) raiseZMQException();
      } else {
        Memory mem = new Memory(msg.length);
        mem.write(0, msg, 0, msg.length);
        if (zmq.zmq_msg_init_data(message, mem, new NativeLong(msg.length), messageDataBuffer, mem) == 0)
          messageDataBuffer.add(mem);
        else raiseZMQException();
      }
      return message;
    }

    private zmq_msg_t newZmqMessage() {
      zmq_msg_t message = new zmq_msg_t();
      if (zmq.zmq_msg_init(message) != 0) raiseZMQException();
      return message;
    }

    private void raiseZMQException() {
      int errno = zmq.zmq_errno();
      throw new ZMQException(zmq.zmq_strerror(errno), errno);
    }

    private class MessageDataBuffer implements zmq_free_fn {
      private HashSet<Pointer> buffer = new HashSet<Pointer>();

      public synchronized void add(Pointer data) {
        buffer.add(data);
      }

      public synchronized void invoke(Pointer data, Pointer memory) {
        buffer.remove(memory);
      }
    }
  }

  public static class Poller {
    public static final int POLLIN = ZeroMQ$.MODULE$.ZMQ_POLLIN();
    public static final int POLLOUT = ZeroMQ$.MODULE$.ZMQ_POLLOUT();
    public static final int POLLERR = ZeroMQ$.MODULE$.ZMQ_POLLERR();

    private static final int SIZE_DEFAULT = 32;
    private static final int SIZE_INCREMENT = 16;
    private static final int UNINITIALIZED_TIMEOUT = -2;

    private long timeout = UNINITIALIZED_TIMEOUT;
    private int nextEventIndex = 0;
    private int maxEventCount = 0;
    private int curEventCount = 0;
    private Socket[] sockets = null;
    private short[] events = null;
    private short[] revents = null;
    private LinkedList<Integer> freeSlots = null;

    public int register(Socket socket) {
      return register(socket, POLLIN | POLLOUT | POLLERR);
    }

    public int register(Socket socket, int numEvents) {
      int pos = -1;
      if (!freeSlots.isEmpty()) pos = freeSlots.remove();
      else {
        if (nextEventIndex >= maxEventCount) {
          int newMaxEventCount = maxEventCount + SIZE_INCREMENT;
          sockets = Arrays.copyOf(sockets, newMaxEventCount);
          events = Arrays.copyOf(events, newMaxEventCount);
          revents = Arrays.copyOf(revents, newMaxEventCount);
          maxEventCount = newMaxEventCount;
        }
        pos = nextEventIndex++;
      }
      sockets[pos] = socket;
      events[pos] = (short) numEvents;
      curEventCount++;
      return pos;
    }

    public void unregister(Socket socket) {
      for (int index = 0; index < nextEventIndex; index++)
        if (sockets[index] == socket) {
          unregisterSocketAtIndex(index);
          break;
        }
    }

    private void unregisterSocketAtIndex(int index) {
      sockets[index] = null;
      events[index] = 0;
      revents[index] = 0;
      freeSlots.add(index);
      curEventCount--;
    }

    public Socket getSocket(int index) {
      return (index < 0 || index >= nextEventIndex) ? null : sockets[index];
    }

    public long getTimeout() {
      return timeout;
    }

    public void setTimeout(long timeout) {
      this.timeout = timeout;
    }

    public int getSize() {
      return maxEventCount;
    }

    public int getNext() {
      return nextEventIndex;
    }

    public long poll() {
      long timeout = -1;
      if (this.timeout != UNINITIALIZED_TIMEOUT) timeout = this.timeout;
      return poll(timeout);
    }

    public long poll(long timeout) {
      int pollItemCount = 0;
      for (int i = 0; i < nextEventIndex; i++) revents[i] = 0;

      if (curEventCount == 0) return 0;

      zmq_pollitem_t[] items = (zmq_pollitem_t[]) new zmq_pollitem_t().toArray(curEventCount);

      for (int i = 0; i < pollItemCount; i++) items[i] = new zmq_pollitem_t();

      for (int socketIndex = 0; socketIndex < sockets.length; socketIndex++) {
        if (sockets[socketIndex] == null) continue;

        items[pollItemCount].socket = sockets[socketIndex].ptr;
        items[pollItemCount].fd = 0;
        items[pollItemCount].events = events[socketIndex];
        items[pollItemCount].revents = 0;
        pollItemCount++;
      }

      if (pollItemCount != curEventCount) return 0;

      pollItemCount = 0;
      // waits for milliseconds, not microseconds
      int result = zmq.zmq_poll(items, curEventCount, new NativeLong(timeout));
      for (int socketIndex = 0; socketIndex < sockets.length; socketIndex++) {
        if (sockets[socketIndex] == null) continue;
        revents[socketIndex] = items[pollItemCount].revents;
        pollItemCount++;
      }
      return result;
    }

    public boolean pollin(int index) {
      return poll_mask(index, POLLIN);
    }

    public boolean pollout(int index) {
      return poll_mask(index, POLLOUT);
    }

    public boolean pollerr(int index) {
      return poll_mask(index, POLLERR);
    }

    protected Poller(Context context) {
      this(context, SIZE_DEFAULT);
    }

    protected Poller(Context context, int size) {
      this.maxEventCount = size;
      this.sockets = new Socket[maxEventCount];
      this.events = new short[maxEventCount];
      this.revents = new short[maxEventCount];
      this.freeSlots = new LinkedList<Integer>();
    }

    private boolean poll_mask(int index, int mask) {
      return !(mask <= 0 || index < 0 || index >= nextEventIndex) && (revents[index] & mask) > 0;
    }
  }
}
