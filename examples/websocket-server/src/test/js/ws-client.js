import WebSocket from "ws";

const url = process.argv[2] || "ws://localhost:8080/tester";
const ws = new WebSocket(url);

console.log("Connecting to:", url);

ws.on("open", () => {
    console.log("CONNECTED");

    // Send a ping every 5 seconds
    setInterval(() => {
        //console.log("→ PING");
       // ws.ping();
    }, 5000);
});

// Normal data messages
ws.on("message", (data) => {
    console.log("← MESSAGE:", data.toString());
});

// Real protocol-level ping from server
ws.on("ping", (data) => {
    console.log("← PING", data?.toString?.() || "");
    ws.ping()
});

// Real protocol-level pong from server
ws.on("pong", (data) => {
    console.log("← PONG", data?.toString?.() || "");
});

ws.on("close", (code, reason) => {
    console.log("CLOSED", code, reason.toString());
});

ws.on("error", (err) => {
    console.error("ERROR:", err);
});
