import { useEffect, useRef, useState } from "react";
import { theme } from "../theme";

export default function OrderBookCanvas({ user }) {
  const canvasRef = useRef(null);
  const [bids, setBids] = useState([]);
  const [asks, setAsks] = useState([]);

  useEffect(() => {
    if (!user) return;
    const bookSource = new EventSource("http://localhost:8080/api/book/stream");

    bookSource.onmessage = (event) => {
      try {
        const snapshot = JSON.parse(event.data);
        setBids(snapshot.bids || []);
        setAsks(snapshot.asks || []);
      } catch (err) {
        console.error("Failed to parse book data:", err);
      }
    };

    return () => bookSource.close();
  }, [user]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    ctx.clearRect(0, 0, canvas.width, canvas.height);
    const rowHeight = 24;
    const midY = canvas.height / 2;

    ctx.textBaseline = "middle";
    ctx.font = '13px "Segoe UI", sans-serif';

    const maxBidQty =
      bids.length > 0 ? Math.max(...bids.map((b) => b.quantity)) : 1;
    const maxAskQty =
      asks.length > 0 ? Math.max(...asks.map((a) => a.quantity)) : 1;
    const maxQty = Math.max(maxBidQty, maxAskQty, 1);

    const maxRows = Math.floor((midY - 20) / rowHeight);
    const visibleAsks = [...asks].slice(0, maxRows).reverse();

    visibleAsks.forEach((ask, index) => {
      const y = midY - 20 - (visibleAsks.length - 1 - index) * rowHeight;
      const barWidth = (ask.quantity / maxQty) * (canvas.width * 0.75);

      ctx.fillStyle = "rgba(246, 70, 93, 0.12)";
      ctx.fillRect(
        canvas.width - barWidth,
        y - rowHeight / 2 + 2,
        barWidth,
        rowHeight - 4,
      );

      ctx.fillStyle = theme.sell;
      ctx.textAlign = "left";
      ctx.fillText(ask.price.toFixed(2), 15, y);

      ctx.fillStyle = theme.textMain;
      ctx.textAlign = "right";
      ctx.fillText(ask.quantity.toFixed(4), canvas.width - 15, y);
    });

    ctx.fillStyle = theme.bgBase;
    ctx.fillRect(0, midY - 15, canvas.width, 30);
    ctx.fillStyle = theme.textMain;
    ctx.textAlign = "left";
    ctx.font = 'bold 13px "Segoe UI", sans-serif';
    ctx.fillText("BTC / USD", 15, midY);
    ctx.fillStyle = theme.textMuted;
    ctx.textAlign = "right";
    ctx.font = '11px "Segoe UI", sans-serif';
    ctx.fillText("Live Spread", canvas.width - 15, midY);

    ctx.font = '13px "Segoe UI", sans-serif';
    const visibleBids = bids.slice(0, maxRows);

    visibleBids.forEach((bid, index) => {
      const y = midY + 20 + index * rowHeight;
      const barWidth = (bid.quantity / maxQty) * (canvas.width * 0.75);

      ctx.fillStyle = "rgba(14, 203, 129, 0.12)";
      ctx.fillRect(
        canvas.width - barWidth,
        y - rowHeight / 2 + 2,
        barWidth,
        rowHeight - 4,
      );

      ctx.fillStyle = theme.buy;
      ctx.textAlign = "left";
      ctx.fillText(bid.price.toFixed(2), 15, y);

      ctx.fillStyle = theme.textMain;
      ctx.textAlign = "right";
      ctx.fillText(bid.quantity.toFixed(4), canvas.width - 15, y);
    });
  }, [bids, asks]);

  return (
    <div
      style={{
        flex: "1",
        minWidth: "300px",
        maxWidth: "350px",
        backgroundColor: theme.bgPanel,
        borderRadius: "4px",
        border: `1px solid ${theme.border}`,
      }}
    >
      <div
        style={{
          padding: "15px 20px",
          borderBottom: `1px solid ${theme.border}`,
        }}
      >
        <h3 style={{ margin: 0, fontSize: "14px", color: theme.textMain }}>
          Order Book
        </h3>
      </div>
      <div style={{ padding: "10px 0" }}>
        <div
          style={{
            display: "flex",
            padding: "0 20px",
            fontSize: "12px",
            color: theme.textMuted,
            marginBottom: "5px",
          }}
        >
          <div style={{ flex: 1 }}>Price (USD)</div>
          <div style={{ flex: 1, textAlign: "right" }}>Amount (BTC)</div>
        </div>
        <div style={{ display: "flex", justifyContent: "center" }}>
          <canvas
            ref={canvasRef}
            width={348}
            height={400}
            style={{ backgroundColor: "transparent" }}
          />
        </div>
      </div>
    </div>
  );
}
