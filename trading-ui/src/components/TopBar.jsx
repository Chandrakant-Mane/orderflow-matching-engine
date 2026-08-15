import { theme } from "../theme";

export default function TopBar({ user, onLogout }) {
  return (
    <div
      style={{
        padding: "15px 25px",
        backgroundColor: theme.bgPanel,
        borderBottom: `1px solid ${theme.border}`,
        display: "flex",
        justifyContent: "space-between",
        alignItems: "center",
      }}
    >
      <h2 style={{ margin: 0, fontSize: "20px", letterSpacing: "1px" }}>
        <span style={{ color: "#f3ba2f" }}>⚡</span> ORDERFLOW{" "}
        <span
          style={{
            color: theme.textMuted,
            fontSize: "14px",
            marginLeft: "10px",
          }}
        >
          PRO TERMINAL
        </span>
      </h2>

      <div style={{ display: "flex", alignItems: "center", gap: "25px" }}>
        <div style={{ fontSize: "13px", color: theme.textMuted }}>
          User:{" "}
          <strong style={{ color: theme.textMain }}>{user.username}</strong>
        </div>
        <div style={{ fontSize: "13px", color: theme.textMuted }}>
          USD Balance:{" "}
          <strong style={{ color: theme.buy }}>
            ${parseFloat(user.balanceUsd).toLocaleString()}
          </strong>
        </div>
        <div style={{ fontSize: "13px", color: theme.textMuted }}>
          BTC Balance:{" "}
          <strong style={{ color: "#f3ba2f" }}>
            {parseFloat(user.balanceBtc).toFixed(4)} BTC
          </strong>
        </div>
        <button
          onClick={onLogout}
          style={{
            padding: "6px 14px",
            backgroundColor: "transparent",
            border: `1px solid ${theme.sell}`,
            color: theme.sell,
            borderRadius: "4px",
            cursor: "pointer",
            fontWeight: "bold",
            fontSize: "12px",
          }}
        >
          Logout
        </button>
      </div>
    </div>
  );
}
