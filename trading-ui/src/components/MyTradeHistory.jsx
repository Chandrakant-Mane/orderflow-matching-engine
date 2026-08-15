import { useState, useEffect } from "react";
import { theme } from "../theme";

export default function MyTradeHistory({ user }) {
  const [trades, setTrades] = useState([]);

  const fetchTradeHistory = async () => {
    try {
      const token = localStorage.getItem("token");
      const response = await fetch(
        "http://localhost:8080/api/orders/me/trades",
        {
          headers: { Authorization: `Bearer ${token}` },
        },
      );
      if (response.ok) {
        const data = await response.json();
        setTrades(data);
      }
    } catch (err) {
      console.error("Failed to fetch trade history:", err);
    }
  };

  useEffect(() => {
    fetchTradeHistory();
    const interval = setInterval(fetchTradeHistory, 3000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div
      style={{
        backgroundColor: theme.bgSurface || "#1e222d",
        borderRadius: "8px",
        padding: "16px",
        flex: "1 1 45%",
        minWidth: "300px",
        border: "1px solid #2a2e39",
      }}
    >
      <h3
        style={{
          margin: "0 0 12px 0",
          fontSize: "16px",
          color: theme.textMain,
        }}
      >
        My Trade History (Fulfilled)
      </h3>
      {trades.length === 0 ? (
        <p style={{ color: "#848e9c", fontSize: "13px" }}>
          No trades executed yet.
        </p>
      ) : (
        <table
          style={{
            width: "100%",
            borderCollapse: "collapse",
            fontSize: "13px",
          }}
        >
          <thead>
            <tr
              style={{
                color: "#848e9c",
                borderBottom: "1px solid #2a2e39",
                textAlign: "left",
              }}
            >
              <th style={{ padding: "6px" }}>Trade ID</th>
              <th style={{ padding: "6px" }}>Buyer</th>
              <th style={{ padding: "6px" }}>Seller</th>
              <th style={{ padding: "6px" }}>Price</th>
              <th style={{ padding: "6px" }}>Qty</th>
            </tr>
          </thead>
          <tbody>
            {trades.map((t) => (
              <tr key={t.id} style={{ borderBottom: "1px solid #2a2e39" }}>
                <td style={{ padding: "6px" }}>#{t.id}</td>
                <td
                  style={{
                    padding: "6px",
                    color:
                      t.buyerUsername === user.username ? "#0ecb81" : "#b7bdc6",
                  }}
                >
                  {t.buyerUsername || "—"}
                </td>
                <td
                  style={{
                    padding: "6px",
                    color:
                      t.sellerUsername === user.username
                        ? "#f6465d"
                        : "#b7bdc6",
                  }}
                >
                  {t.sellerUsername || "—"}
                </td>
                <td style={{ padding: "6px" }}>${t.price}</td>
                <td style={{ padding: "6px" }}>{t.quantity} BTC</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
