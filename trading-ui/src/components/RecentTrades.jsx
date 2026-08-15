import { useEffect, useState } from "react";
import { theme } from "../theme";

export default function RecentTrades({ user }) {
  const [trades, setTrades] = useState([]);

  useEffect(() => {
    if (!user) return;
    const tradeSource = new EventSource(
      "http://localhost:8080/api/trades/stream",
    );

    tradeSource.onmessage = (event) => {
      try {
        const newTrade = JSON.parse(event.data);
        setTrades((prevTrades) => [newTrade, ...prevTrades].slice(0, 50));
      } catch (err) {
        console.error("Failed to parse trade data:", err);
      }
    };

    return () => tradeSource.close();
  }, [user]);

  const formatTime = (timestamp) => {
    const date = new Date(timestamp);
    return date.toLocaleTimeString("en-US", { hour12: false });
  };

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
          Recent Trades
        </h3>
      </div>
      <div style={{ padding: "10px 0" }}>
        <div
          style={{
            display: "flex",
            padding: "0 20px",
            fontSize: "12px",
            color: theme.textMuted,
            marginBottom: "10px",
          }}
        >
          <div style={{ flex: 1 }}>Price (USD)</div>
          <div style={{ flex: 1, textAlign: "right" }}>Amount</div>
          <div style={{ flex: 1, textAlign: "right" }}>Time</div>
        </div>
        <div style={{ maxHeight: "400px", overflowY: "auto" }}>
          {trades.length === 0 ? (
            <div
              style={{
                padding: "20px",
                textAlign: "center",
                color: theme.textMuted,
                fontSize: "13px",
              }}
            >
              Waiting for trades...
            </div>
          ) : (
            trades.map((trade, index) => (
              <div
                key={index}
                style={{
                  display: "flex",
                  padding: "6px 20px",
                  fontSize: "13px",
                  backgroundColor:
                    index % 2 === 0 ? "transparent" : "rgba(255,255,255,0.02)",
                }}
              >
                <div
                  style={{ flex: 1, color: theme.textMain, fontWeight: "500" }}
                >
                  {trade.price.toFixed(2)}
                </div>
                <div
                  style={{ flex: 1, textAlign: "right", color: theme.textMain }}
                >
                  {trade.quantity.toFixed(4)}
                </div>
                <div
                  style={{
                    flex: 1,
                    textAlign: "right",
                    color: theme.textMuted,
                  }}
                >
                  {formatTime(trade.timestamp)}
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
