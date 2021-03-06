package org.thoughtcrime.redphone.audio;

import android.os.Build;
import android.util.Log;


import org.thoughtcrime.securesms.R;

import java.io.FileDescriptor;
import java.lang.reflect.Field;
import java.net.DatagramSocket;
import java.net.DatagramSocketImpl;
import java.net.SocketException;

public class CallAudioManager {

  private static final String TAG = CallAudioManager.class.getSimpleName();

  static {
    System.loadLibrary("redphone-audio");
  }

  private final long handle;

  public CallAudioManager(DatagramSocket socket, String remoteHost, int remotePort,
                          byte[] senderCipherKey, byte[] senderMacKey, byte[] senderSalt,
                          byte[] receiverCipherKey, byte[] receiverMacKey, byte[] receiverSalt)
      throws SocketException, AudioException
  {
    try {
      this.handle = create(Build.VERSION.SDK_INT, getFileDescriptor(socket), remoteHost, remotePort,
                           senderCipherKey, senderMacKey, senderSalt,
                           receiverCipherKey, receiverMacKey, receiverSalt);
    } catch (NativeAudioException e) {
      Log.w(TAG, e);
      throw new AudioException("Sorry, there was a problem initiating audio on your device");
    }
  }

  public void setMute(boolean enabled) {
    setMute(handle, enabled);
  }

  public void start() throws AudioException {
    try {
      start(handle);
    } catch (NativeAudioException e) {
      Log.w(TAG, e);
      throw new AudioException("sorry_there_was_a_problem_initializing_the_audio_on_your_device");
    }
  }

  public void terminate() {
    stop(handle);
    dispose(handle);
  }

  private static int getFileDescriptor(DatagramSocket socket) throws SocketException {
    try {
      socket.setSoTimeout(5000);
      Field implField = DatagramSocket.class.getDeclaredField("impl");
      implField.setAccessible(true);

      DatagramSocketImpl implValue = (DatagramSocketImpl)implField.get(socket);

      Field fdField = DatagramSocketImpl.class.getDeclaredField("fd");
      fdField.setAccessible(true);

      FileDescriptor fdValue = (FileDescriptor)fdField.get(implValue);

      Field descField = FileDescriptor.class.getDeclaredField("descriptor");
      descField.setAccessible(true);

      return (Integer)descField.get(fdValue);
    } catch (NoSuchFieldException e) {
      throw new AssertionError(e);
    } catch (IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  private native long create(int androidSdkVersion,
                             int socketFd, String serverIpString, int serverPort,
                             byte[] senderCipherKey, byte[] senderMacKey, byte[] senderSalt,
                             byte[] receiverCipherKey, byte[] receiverMacKey, byte[] receiverSalt)
      throws NativeAudioException;

  private native void start(long handle) throws NativeAudioException;

  private native void setMute(long handle, boolean enabled);

  private native void stop(long handle);

  private native void dispose(long handle);

}
