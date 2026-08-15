const BASE_URL = "http://localhost:8080/api";

export const api = {
  async submitOrder(orderPayload) {
    const token = localStorage.getItem("token");

    const response = await fetch(`${BASE_URL}/orders`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(orderPayload),
    });

    const contentType = response.headers.get("content-type");
    let data = {};

    if (contentType && contentType.includes("application/json")) {
      data = await response.json();
    } else {
      const text = await response.text();
      console.error("Non-JSON Server Error:", text);
      throw new Error("Server Error or Expired Token. Please log in again.");
    }

    if (!response.ok) {
      throw new Error(data.reason || data.error || "Risk check failed");
    }

    return data;
  },
};
