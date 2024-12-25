let ws;
function connectWebSocket() {
    ws = new WebSocket("ws://localhost:8081/ws");

    ws.onopen = () => {
        console.log("WebSocket connected");
        ws.send(JSON.stringify({ type: "connect", data: "Client connected" }));
    }
    ws.onmessage = (event) => {
        const message = JSON.parse(event.data);
        console.log("Message from server:", message);
    };
    ws.onclose = () => {
        console.log("WebSocket disconnected, retrying...");
        setTimeout(connectWebSocket, 5000); // Reconnect after 5 seconds
    };
    ws.onerror = (error) => console.error("WebSocket error:", error);
}

// Send user interaction to the server
function sendUserInteraction(interaction) {
    ws.send(JSON.stringify({ type: "interaction", data: interaction }));
}


connectWebSocket();