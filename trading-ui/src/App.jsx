import { useState, useEffect } from "react";
import AuthModal from "./components/AuthModal";
import TopBar from "./components/TopBar";
import OrderEntry from "./components/OrderEntry";
import OrderBookCanvas from "./components/OrderBookCanvas";
import RecentTrades from "./components/RecentTrades";
import MyActiveOrders from "./components/MyActiveOrders";
import MyTradeHistory from "./components/MyTradeHistory";
import { theme } from "./theme";

export default function App() {
  const [user, setUser] = useState(null);

  // Check for existing session on mount
  useEffect(() => {
    const token = localStorage.getItem("token");
    const username = localStorage.getItem("username");
    if (token && username) {
      setUser({
        username,
        balanceUsd: localStorage.getItem("balanceUsd") || "100000.00",
        balanceBtc: localStorage.getItem("balanceBtc") || "10.00",
      });
    }
  }, []);

  // Background Profile Sync
  useEffect(() => {
    if (!user) return;

    const fetchProfile = async () => {
      try {
        const token = localStorage.getItem("token");
        const response = await fetch(
          "http://localhost:8080/api/orders/profile",
          {
            headers: { Authorization: `Bearer ${token}` },
          },
        );
        if (response.ok) {
          const data = await response.json();
          setUser((prev) => {
            if (!prev) return prev;
            if (
              prev.balanceUsd !== data.balanceUsd ||
              prev.balanceBtc !== data.balanceBtc
            ) {
              localStorage.setItem("balanceUsd", data.balanceUsd);
              localStorage.setItem("balanceBtc", data.balanceBtc);
              return {
                ...prev,
                balanceUsd: data.balanceUsd,
                balanceBtc: data.balanceBtc,
              };
            }
            return prev;
          });
        }
      } catch (err) {
        console.error("Failed to sync profile balances:", err);
      }
    };

    fetchProfile();
    const interval = setInterval(fetchProfile, 3000);

    return () => clearInterval(interval);
  }, [user?.username]);

  const handleLogout = () => {
    localStorage.clear();
    setUser(null);
  };

  const handleOrderSuccess = (updatedUsd, updatedBtc) => {
    setUser((prev) => ({
      ...prev,
      balanceUsd: updatedUsd,
      balanceBtc: updatedBtc,
    }));
    localStorage.setItem("balanceUsd", updatedUsd);
    localStorage.setItem("balanceBtc", updatedBtc);
  };

  if (!user) {
    return <AuthModal onLoginSuccess={setUser} />;
  }

  return (
    <div
      style={{
        fontFamily: '"Segoe UI", Roboto, Helvetica, Arial, sans-serif',
        backgroundColor: theme.bgBase,
        color: theme.textMain,
        minHeight: "100vh",
        display: "flex",
        flexDirection: "column",
      }}
    >
      <TopBar user={user} onLogout={handleLogout} />

      {/* Main Trading Area */}
      <div
        style={{
          display: "flex",
          gap: "20px",
          padding: "20px",
          flexWrap: "wrap",
          alignItems: "flex-start",
        }}
      >
        <OrderEntry user={user} onOrderSuccess={handleOrderSuccess} />
        <OrderBookCanvas user={user} />
        <RecentTrades user={user} />
      </div>

      {/* User Portfolio & Personal History Section */}
      <div
        style={{
          display: "flex",
          gap: "20px",
          padding: "0 20px 20px 20px",
          flexWrap: "wrap",
        }}
      >
        <MyActiveOrders user={user} />
        <MyTradeHistory user={user} />
      </div>
    </div>
  );
}
