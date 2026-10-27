# enet-j2me

A client-only realization of enet 1.3.x proto for CLDC 1.1 / MIDP 2.0.
Based on [zpl-c/enet](https://github.com/zpl-c/enet).

> [!Important] 
> The project is **fully vibecoded** but tested. Use it at your own risk.

# Using
```java
ENetClient c = new ENetClient(new enet.midp.DatagramTransport("192.168.0.10", 7777));
c.connect(2, 0);
for (;;) {
    ENetEvent e = c.service(50);
    if (e == null) continue;
    if (e.type == ENetEvent.CONNECT) c.send(0, "hi".getBytes(), ENetPacket.FLAG_RELIABLE);
    if (e.type == ENetEvent.RECEIVE)    { byte[] data = e.packet.data; }
    if (e.type == ENetEvent.DISCONNECT) break;
}
c.close();
```

# Test
1. Build enet server
```bash
cd server && gcc echo_server.c -o echo_server -DENET_OLD_API -Ienet/include # add -lws2_32 -lwinmm if you are on windows
./echo_server 7777
```
2. Build j2me client and run it on real phone or on the latest version of [KEmulator](https://github.com/shinovon/KEmulator).