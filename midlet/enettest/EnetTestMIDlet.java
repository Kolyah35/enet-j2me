import enet.ENet;
import enet.ENetClient;
import enet.ENetEvent;
import enet.ENetPacket;
import enet.midp.DatagramTransport;

import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;
import java.io.IOException;

public class EnetTestMIDlet extends MIDlet implements CommandListener {
    private Form form;
    private final Command exitCmd = new Command("Exit", Command.EXIT, 1);
    private final Command connectCmd = new Command("Connect", Command.OK, 1);
    private final TextField serverHost = new TextField("Server Host", "192.168.0.100:7777", 255, TextField.ANY);
    private int failed;

    protected void startApp() {
        form = new Form("ENet test");
        form.addCommand(exitCmd);
        form.addCommand(connectCmd);
        form.append(serverHost);

        form.setCommandListener(this);
        Display.getDisplay(this).setCurrent(form);
    }

    protected void pauseApp() {}

    protected void destroyApp(boolean unconditional) {}

    public void commandAction(Command c, Displayable d) {
        if (c == exitCmd) {
            destroyApp(true);
            notifyDestroyed();
        }

        if (c == connectCmd) {
            String host = serverHost.getString();

            int port = 7777;
            {
                int idx = host.indexOf(":");

                if (idx > 0){
                    port = Integer.parseInt(host.substring(idx + 1));
                    host = host.substring(0, idx);
                }
            }

            form.append("Server " + host + ":" + port + "\n");

            try {
                this.runTest(host, port);
            } catch (IOException e) {
                throw new RuntimeException();
            }
        }
    }

    private void log(String s) {
        form.append(s + "\n");
    }

    private void result(String name, boolean ok, String info) {
        if (!ok) failed++;
        log((ok ? "PASS " : "FAIL ") + name + (info == null ? "" : " (" + info + ")"));
    }

    private static ENetEvent waitFor(ENetClient c, int type, int ms) {
        long end = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < end) {
            ENetEvent e = c.service(50);
            if (e == null) continue;
            if (e.type == type) return e;
            if (e.type == ENetEvent.DISCONNECT) return null;
        }
        return null;
    }

    private static boolean same(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) if (a[i] != b[i]) return false;
        return true;
    }

    private static byte[] pattern(int n) {
        byte[] b = new byte[n];
        for (int i = 0; i < n; i++) b[i] = (byte) (i * 7 + 3);
        return b;
    }

    public void runTest(String host, int port) throws IOException {
        DatagramTransport t = null;
        ENetClient c = null;
        try {
            t = new DatagramTransport(host, port);
            c = new ENetClient(t);
            int max = t.getMaximumLength();
            if (max >= ENet.MIN_MTU && max < ENet.DEFAULT_MTU) c.setMtu(max);
            log("datagram max=" + max);

            c.connect(2, 1234);
            ENetEvent e = waitFor(c, ENetEvent.CONNECT, 5000);
            result("connect", e != null, e == null ? "no reply from server" : "mtu=" + c.getMtu());
            if (e == null) return;

            byte[] small = pattern(100);
            c.send(0, small, ENetPacket.FLAG_RELIABLE);
            e = waitFor(c, ENetEvent.RECEIVE, 4000);
            result("reliable echo", e != null && same(e.packet.data, small), null);

            byte[] big = pattern(8000);
            c.send(1, big, ENetPacket.FLAG_RELIABLE);
            e = waitFor(c, ENetEvent.RECEIVE, 8000);
            result("fragmented echo 8000B", e != null && same(e.packet.data, big), null);

            boolean ok = false;
            byte[] u = pattern(40);
            for (int attempt = 0; attempt < 3 && !ok; attempt++) {
                c.send(0, u, 0);
                e = waitFor(c, ENetEvent.RECEIVE, 1500);
                ok = e != null && same(e.packet.data, u);
            }
            result("unreliable echo", ok, null);

            c.disconnect(77);
            e = waitFor(c, ENetEvent.DISCONNECT, 4000);
            result("disconnect", e != null && e.data == 77, null);
        } catch (RuntimeException ex) {
            result("exception", false, ex.toString());
        } finally {
            if (c != null) c.close();
            log(failed == 0 ? "ALL PASSED" : failed + " FAILED");
        }
    }
}
