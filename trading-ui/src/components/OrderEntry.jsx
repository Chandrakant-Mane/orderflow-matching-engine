import { useState } from "react";
import { theme } from "../theme";
import { api } from "../services/api";

export default function OrderEntry({ user, onOrderSuccess }) {
  const [price, setPrice] = useState("");
  const [quantity, setQuantity] = useState("");
  const [side, setSide] = useState("B");

  const handleSubmit = async (e) => {
    e.preventDefault();
    const orderPayload = {
      username: user.username,
      clientOrderId: Date.now().toString(),
      symbol: "BTC-USD",
      price: parseFloat(price),
      quantity: parseFloat(quantity),
      side: side === "B" ? "BUY" : "SELL",
    };

    try {
      const data = await api.submitOrder(orderPayload);
      onOrderSuccess(data.updatedBalanceUsd, data.updatedBalanceBtc);
      setPrice("");
      setQuantity("");
    } catch (error) {
      alert(`❌ Order Rejected: ${error.message}`);
    }
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
        padding: "20px",
      }}
    >
      <h3
        style={{
          margin: "0 0 20px 0",
          fontSize: "16px",
          color: theme.textMain,
        }}
      >
        Place Order
      </h3>
      <div style={{ display: "flex", gap: "10px", marginBottom: "20px" }}>
        <button
          onClick={() => setSide("B")}
          style={{
            flex: 1,
            padding: "10px",
            fontWeight: "bold",
            borderRadius: "4px",
            cursor: "pointer",
            backgroundColor: side === "B" ? theme.buy : "transparent",
            color: side === "B" ? "#fff" : theme.buy,
            border: side === "B" ? "none" : `1px solid ${theme.buy}`,
          }}
        >
          Buy
        </button>
        <button
          onClick={() => setSide("S")}
          style={{
            flex: 1,
            padding: "10px",
            fontWeight: "bold",
            borderRadius: "4px",
            cursor: "pointer",
            backgroundColor: side === "S" ? theme.sell : "transparent",
            color: side === "S" ? "#fff" : theme.sell,
            border: side === "S" ? "none" : `1px solid ${theme.sell}`,
          }}
        >
          Sell
        </button>
      </div>

      <form
        onSubmit={handleSubmit}
        style={{ display: "flex", flexDirection: "column", gap: "15px" }}
      >
        <div style={{ display: "flex", flexDirection: "column" }}>
          <label
            style={{
              fontSize: "12px",
              color: theme.textMuted,
              marginBottom: "5px",
            }}
          >
            Price (USD)
          </label>
          <input
            type="number"
            step="0.01"
            required
            value={price}
            onChange={(e) => setPrice(e.target.value)}
            style={{
              padding: "10px",
              backgroundColor: theme.bgBase,
              border: `1px solid ${theme.border}`,
              color: theme.textMain,
              borderRadius: "4px",
              outline: "none",
            }}
          />
        </div>
        <div style={{ display: "flex", flexDirection: "column" }}>
          <label
            style={{
              fontSize: "12px",
              color: theme.textMuted,
              marginBottom: "5px",
            }}
          >
            Quantity (BTC)
          </label>
          <input
            type="number"
            step="any"
            required
            value={quantity}
            onChange={(e) => setQuantity(e.target.value)}
            style={{
              padding: "10px",
              backgroundColor: theme.bgBase,
              border: `1px solid ${theme.border}`,
              color: theme.textMain,
              borderRadius: "4px",
              outline: "none",
            }}
          />
        </div>
        <button
          type="submit"
          style={{
            marginTop: "10px",
            padding: "12px",
            backgroundColor: side === "B" ? theme.buy : theme.sell,
            color: "white",
            border: "none",
            borderRadius: "4px",
            cursor: "pointer",
            fontWeight: "bold",
          }}
        >
          {side === "B" ? "Buy BTC" : "Sell BTC"}
        </button>
      </form>
    </div>
  );
}
