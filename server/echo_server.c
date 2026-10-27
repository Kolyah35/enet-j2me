/*
 * Echo server for testing the J2ME ENet client against REAL zpl-c/enet.
 *
 * Build (Linux/macOS):  gcc echo_server.c -o echo_server
 * Build (Windows MinGW): gcc echo_server.c -o echo_server.exe -lws2_32 -lwinmm
 * Put zpl-c's single header "enet.h" next to this file (https://github.com/zpl-c/enet).
 * Run: ./echo_server [port]       (default 7777)
 *
 * Echoes every packet back on the same channel with the same reliability.
 *
 * API note: zpl-c/enet >= 2.4 takes an address type as first argument of
 * enet_host_create(). For older versions compile with -DENET_OLD_API.
 */
#define ENET_IMPLEMENTATION
#include "enet.h"

#include <stdio.h>
#include <stdlib.h>

int main(int argc, char **argv) {
    int port = argc > 1 ? atoi(argv[1]) : 7777;

    if (enet_initialize() != 0) {
        fprintf(stderr, "enet_initialize failed\n");
        return 1;
    }

    ENetAddress address = {0};
    address.host = ENET_HOST_ANY;
    address.port = (enet_uint16)port;

#ifdef ENET_OLD_API
    ENetHost *server = enet_host_create(&address, 32, 4, 0, 0);
#else
    ENetHost *server = enet_host_create(ENET_ADDRESS_TYPE_ANY, &address, 32, 4, 0, 0);
#endif
    if (!server) {
        fprintf(stderr, "enet_host_create failed (port in use?)\n");
        return 1;
    }

    /* If you want to test the client's setChecksum(true) mode, uncomment (if your
     * zpl-c version exposes it):  server->checksum = enet_crc32;  */

    printf("echo server listening on UDP %d\n", port);

    ENetEvent event;
    for (;;) {
        while (enet_host_service(server, &event, 100) > 0) {
            switch (event.type) {
                case ENET_EVENT_TYPE_CONNECT:
                    printf("connect: data=%u\n", (unsigned)event.data);
                    break;

                case ENET_EVENT_TYPE_RECEIVE: {
                    enet_uint32 flags = event.packet->flags &
                        (ENET_PACKET_FLAG_RELIABLE | ENET_PACKET_FLAG_UNSEQUENCED);
                    printf("recv: channel=%u len=%u flags=%u\n", (unsigned)event.channelID,
                           (unsigned)event.packet->dataLength, (unsigned)event.packet->flags);
                    ENetPacket *p = enet_packet_create(event.packet->data,
                                                       event.packet->dataLength, flags);
                    enet_peer_send(event.peer, event.channelID, p);
                    enet_packet_destroy(event.packet);
                } break;

                case ENET_EVENT_TYPE_DISCONNECT:
                    printf("disconnect: data=%u\n", (unsigned)event.data);
                    break;

                default:
                    break;
            }
        }
    }

    enet_host_destroy(server);
    enet_deinitialize();
    return 0;
}
