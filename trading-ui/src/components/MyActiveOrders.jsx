import { useState, useEffect } from "react";
import { theme } from "../theme";

export default function MyActiveOrders({ user }) {
  const [orders, setOrders] = useState([]);

  const fetchActiveOrders = async () => {
    try {
      const token = localStorage.getItem("token");
      const response = await fetch(
        "http://localhost:8080/api/orders/me/active",
        {
          headers: { Authorization: `Bearer ${token}` },
        },
      );
      if (response.ok) {
        const data = await response.json();
        setOrders(data);
      }
    } catch (err) {
      console.error("Failed to fetch active orders:", err);
    }
  };

  useEffect(() => {
    fetchActiveOrders();
    const interval = setInterval(fetchActiveOrders, 3000);
    return () => clearInterval(interval);
  }, []);

  // NEW: Handle order cancellation
  const handleCancel = async (orderId) => {
    if (!window.confirm("Are you sure you want to cancel this order?")) return;

    try {
      const token = localStorage.getItem("token");
      const response = await fetch(
        `http://localhost:8080/api/orders/${orderId}`,
        {
          method: "DELETE",
          headers: { Authorization: `Bearer ${token}` },
        },
      );

      if (response.ok) {
        // Immediately fetch to remove the cancelled order from the list
        fetchActiveOrders();
      } else {
        const data = await response.json();
        alert(`Cancel failed: ${data.error || data.reason}`);
      }
    } catch (err) {
      console.error("Failed to cancel order:", err);
    }
  };

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
        My Active Orders (Waiting)
      </h3>
      {orders.length === 0 ? (
        <p style={{ color: "#848e9c", fontSize: "13px" }}>
          No active waiting orders.
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
              <th style={{ padding: "6px" }}>ID</th>
              <th style={{ padding: "6px" }}>Side</th>
              <th style={{ padding: "6px" }}>Price</th>
              <th style={{ padding: "6px" }}>Qty</th>
              <th style={{ padding: "6px" }}>Status</th>
              {/* NEW: Action Column Header */}
              <th style={{ padding: "6px", textAlign: "right" }}>Action</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((o) => (
              <tr key={o.id} style={{ borderBottom: "1px solid #2a2e39" }}>
                <td style={{ padding: "6px" }}>#{o.id}</td>
                <td
                  style={{
                    padding: "6px",
                    color: o.side === "BUY" ? "#0ecb81" : "#f6465d",
                    fontWeight: "bold",
                  }}
                >
                  {o.side}
                </td>
                <td style={{ padding: "6px" }}>${o.price}</td>
                <td style={{ padding: "6px" }}>{o.quantity} BTC</td>
                <td style={{ padding: "6px", color: "#f0b90b" }}>{o.status}</td>

                {/* NEW: Cancel Button Cell */}
                <td style={{ padding: "6px", textAlign: "right" }}>
                  <button
                    onClick={() => handleCancel(o.id)}
                    style={{
                      backgroundColor: "transparent",
                      color: "#f6465d",
                      border: "1px solid #f6465d",
                      borderRadius: "4px",
                      padding: "4px 10px",
                      cursor: "pointer",
                      fontSize: "12px",
                      transition: "0.2s",
                    }}
                    onMouseOver={(e) => {
                      e.target.style.backgroundColor = "#f6465d";
                      e.target.style.color = "#fff";
                    }}
                    onMouseOut={(e) => {
                      e.target.style.backgroundColor = "transparent";
                      e.target.style.color = "#f6465d";
                    }}
                  >
                    Cancel
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
