import React, { useEffect, useRef, useState } from "react";

export const OrderBookCanvas = () => {
  const canvasRef = useRef(null);
  const [bookData, setBookData] = useState(null);

  // Establish SSE connection to the backend Spring WebFlux stream
  useEffect(() => {
    const eventSource = new EventSource(
      "http://localhost:8080/api/book/stream",
    );

    eventSource.onmessage = (event) => {
      try {
        const snapshot = JSON.parse(event.data);
        setBookData(snapshot);
      } catch (error) {
        console.error("Failed to parse order book stream data:", error);
      }
    };

    eventSource.onerror = (error) => {
      console.error("SSE connection error:", error);
      eventSource.close();
    };

    return () => {
      eventSource.close();
    };
  }, []);

  // Render the order book depth on the HTML5 Canvas
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    // Clear canvas
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    if (!bookData || !bookData.bids || !bookData.asks) return;

    const width = canvas.width;
    const height = canvas.height;
    const halfHeight = height / 2;

    // Fixed: Use 'quantity' instead of 'totalQuantity' (Checkpoint 7)
    const maxBidQty =
      bookData.bids.length > 0
        ? Math.max(...bookData.bids.map((b) => b.quantity))
        : 1;
    const maxAskQty =
      bookData.asks.length > 0
        ? Math.max(...bookData.asks.map((a) => a.quantity))
        : 1;
    const maxQty = Math.max(maxBidQty, maxAskQty, 1);

    const rowHeight = 20;

    // --- Draw Asks (Top Half - Red/Pink tone) ---
    ctx.font = "12px Inter, monospace";
    bookData.asks
      .slice(0, 10)
      .reverse()
      .forEach((ask, index) => {
        const y = index * rowHeight + 15;
        const barWidth = (ask.quantity / maxQty) * (width * 0.6);

        // Depth background bar
        ctx.fillStyle = "rgba(239, 68, 68, 0.15)";
        ctx.fillRect(width - barWidth, y - 12, barWidth, rowHeight - 2);

        // Price text
        ctx.fillStyle = "#ef4444";
        ctx.fillText(ask.price.toFixed(2), 10, y);

        // Quantity text
        ctx.fillStyle = "#9ca3af";
        ctx.textAlign = "right";
        ctx.fillText(ask.quantity.toFixed(4), width - 10, y);
        ctx.textAlign = "left";
      });

    // --- Draw Bids (Bottom Half - Green tone) ---
    bookData.bids.slice(0, 10).forEach((bid, index) => {
      const y = halfHeight + index * rowHeight + 15;
      const barWidth = (bid.quantity / maxQty) * (width * 0.6);

      // Depth background bar
      ctx.fillStyle = "rgba(34, 197, 94, 0.15)";
      ctx.fillRect(width - barWidth, y - 12, barWidth, rowHeight - 2);

      // Price text
      ctx.fillStyle = "#22c55e";
      ctx.fillText(bid.price.toFixed(2), 10, y);

      // Quantity text
      ctx.fillStyle = "#9ca3af";
      ctx.textAlign = "right";
      ctx.fillText(bid.quantity.toFixed(4), width - 10, y);
      ctx.textAlign = "left";
    });
  }, [bookData]);

  return (
    <div
      className="order-book-container"
      style={{ background: "#121214", padding: "10px", borderRadius: "8px" }}
    >
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          color: "#9ca3af",
          fontSize: "12px",
          marginBottom: "8px",
          borderBottom: "1px solid #27272a",
          paddingBottom: "4px",
        }}
      >
        <span>Price (USD)</span>
        <span>Amount (BTC)</span>
      </div>
      <canvas
        ref={canvasRef}
        width={320}
        height={400}
        style={{
          width: "100%",
          height: "auto",
          background: "transparent",
          display: "block",
        }}
      />
    </div>
  );
};
