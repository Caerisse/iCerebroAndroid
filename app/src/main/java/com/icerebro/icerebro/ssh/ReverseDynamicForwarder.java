/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.icerebro.icerebro.ssh;

/**
 *
 * @author shaia
 */

import android.util.Log;

import com.jcraft.jsch.ChannelDirectTCPIP;
import com.jcraft.jsch.Session;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import jsocks.socks.CProxy;
import jsocks.socks.ProxyMessage;
import jsocks.socks.ProxyServer;
import jsocks.socks.Socks4Message;
import jsocks.socks.Socks5Message;
import jsocks.socks.SocksSocket;
import jsocks.socks.server.ServerAuthenticatorNone;

class ReverseDynamicForwarder implements Runnable {
    private String TAG = "ReverseDynamicForwarder";

    private class ReverseDynamicForward extends ProxyServer {
        int port;

        ReverseDynamicForward(int port) {
            super(new ServerAuthenticatorNone());
            this.port = port;
            setLog(System.out);
        }

        ReverseDynamicForward(Socket s, int port) {
            super(new ServerAuthenticatorNone(), s);
            this.port = port;
            setLog(System.out);
        }

        @Override
        public void start(int port, int backlog, InetAddress localIP) throws IOException {
            ss = new ServerSocket(port, backlog, localIP);
            Log.i(TAG,"Starting SOCKS Proxy on:"+ss.getInetAddress().getHostAddress()+":"
                    +ss.getLocalPort());

            //noinspection InfiniteLoopStatement
            while (true) {
                Socket s = ss.accept();
                Log.i(TAG,"Accepted from:"+s.getInetAddress().getHostName()+":"
                        +s.getPort());
                ReverseDynamicForward rdf = new ReverseDynamicForward(s, port);
                (new Thread(rdf)).start();
            }
        }

        @Override
        protected void onConnect(ProxyMessage msg) throws IOException {
            Log.d(TAG, "onConnect");
            ProxyMessage response = null;
            int iSock5Cmd = CProxy.SOCKS_FAILURE;    //defaulting to failure
            int iSock4Msg = Socks4Message.REPLY_NO_CONNECT;
            InetAddress sIp = null;
            int iPort = 0;

            Socket s = null;

            try {
                if (proxy == null) {
                    s = new Socket(msg.ip, msg.port);
                } else {
                    s = new SocksSocket(proxy, msg.ip, msg.port);
                }
                log("Connected to " + s.getInetAddress() + ":" + s.getPort());

                iSock5Cmd = CProxy.SOCKS_SUCCESS; iSock4Msg = Socks4Message.REPLY_OK;
                sIp = s.getInetAddress();
                iPort = s.getPort();

            }
            catch (Exception sE) {
                log("Failed connecting to remote socket. Exception: " + sE.getLocalizedMessage());

                //TBD Pick proper socks error for corresponding socket error, below is too generic
                iSock5Cmd = CProxy.SOCKS_CONNECTION_REFUSED; iSock4Msg = Socks4Message.REPLY_NO_CONNECT;
            }

            if (msg instanceof Socks5Message) {
                response = new Socks5Message(iSock5Cmd, sIp, iPort);
            } else {
                response = new Socks4Message(iSock4Msg, sIp, iPort);
            }

            response.write(out);

            if (s != null) {
                startPipe(s);
            }
            else {
                throw (new RuntimeException("onConnect() Failed to create Socket()"));
            }

        }

        void start() throws IOException {
            Log.d(TAG, "Starting ReverseDynamicForward");
            start(port);
        }
    }

    private ReverseDynamicForward rdf = null;
    private Thread ServerThread = null;
    private Exception e = null;


    ReverseDynamicForwarder(int port) {
        rdf = new ReverseDynamicForward(port);
        ReverseDynamicForward.setLog(System.out);
        ServerThread = new Thread(this, "ReverseDynamicForwarder");
        ServerThread.start();
    }

    public void run() {
        try {
            rdf.start();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            this.e = e;
        }
    }

    void stop() {
        rdf.stop();
        if (!ServerThread.isInterrupted()) {
            ServerThread.interrupt();
        }
    }
}
