import { useState, useEffect, useRef } from 'react'
import axios from 'axios'
import SockJS from 'sockjs-client'
import * as Stomp from 'stompjs'
import './index.css'

const ORDER_API_URL = import.meta.env.VITE_ORDER_API_URL || 'http://localhost:8080'
const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080'

function App() {
  const [formData, setFormData] = useState({
    symbol: 'AAPL',
    side: 'BUY',
    quantity: 100,
    price: 150.0,
    orderType: 'LIMIT'
  })
  const [events, setEvents] = useState([])
  const [ticks, setTicks] = useState([])
  const [isConnected, setIsConnected] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const stompClientRef = useRef(null)

  useEffect(() => {
    connectWebSocket()
    return () => {
      disconnectWebSocket()
    }
  }, [])

  const connectWebSocket = () => {
    try {
      const socket = new SockJS(`${WS_URL}/ws/orders`)
      const client = Stomp.over(socket)
      
      // Enable debug logging to see what's happening
      client.debug = function(str) {
        console.log('STOMP:', str)
      }
      
      // Set heartbeat to keep connection alive
      client.heartbeat.outgoing = 10000
      client.heartbeat.incoming = 10000
      
      // Handle socket events directly
      socket.onopen = function() {
        console.log('Socket opened')
      }
      
      socket.onclose = function() {
        console.log('Socket closed')
        setIsConnected(false)
        // Attempt reconnect after 3 seconds
        setTimeout(connectWebSocket, 3000)
      }
      
      socket.onerror = function(error) {
        console.error('Socket error:', error)
        setIsConnected(false)
      }
      
      // Connect with proper callbacks
      client.connect(
        {}, // Headers
        function(frame) {
          // Success callback
          console.log('Connected: ' + frame)
          setIsConnected(true)
          
          // Subscribe to order events
          client.subscribe('/topic/orders', function(message) {
            try {
              const event = JSON.parse(message.body)
              console.log('Received event:', event)
              addEvent(event)
            } catch (error) {
              console.error('Error parsing event:', error)
            }
          })
        },
        function(error) {
          // Error callback
          console.error('STOMP error:', error)
          setIsConnected(false)
        }
      )

      stompClientRef.current = client
    } catch (error) {
      console.error('Error setting up WebSocket:', error)
      setIsConnected(false)
    }
  }

  const disconnectWebSocket = () => {
    if (stompClientRef.current) {
      stompClientRef.current.disconnect()
      setIsConnected(false)
    }
  }

  const addEvent = (event) => {
    setEvents(prev => [event, ...prev].slice(0, 100)) // Keep last 100 events
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setIsSubmitting(true)

    try {
      const response = await axios.post(`${ORDER_API_URL}/api/v1/orders`, formData)
      console.log('Order submitted:', response.data)
      addEvent({
        eventId: `temp-${Date.now()}`,
        orderId: response.data.orderId,
        status: 'PENDING',
        message: 'Order submitted',
        timestamp: new Date().toISOString()
      })
    } catch (error) {
      console.error('Error submitting order:', error)
      addEvent({
        eventId: `error-${Date.now()}`,
        orderId: 'N/A',
        status: 'ERROR',
        message: error.response?.data?.message || 'Failed to submit order',
        timestamp: new Date().toISOString()
      })
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleChange = (e) => {
    const { name, value } = e.target
    setFormData(prev => ({
      ...prev,
      [name]: name === 'quantity' || name === 'price' ? parseFloat(value) : value
    }))
  }

  const formatTimestamp = (timestamp) => {
    if (!timestamp) return ''
    const date = new Date(timestamp)
    return date.toLocaleTimeString()
  }

  const getStatusClass = (status) => {
    if (status === 'ACCEPTED') return 'accepted'
    if (status === 'FILLED') return 'filled'
    if (status === 'REJECTED' || status === 'ERROR') return 'rejected'
    return ''
  }

  return (
    <div className="app">
      <div className="header">
        <h1>Trading POC - Order Management System</h1>
        <div className="status">
          <span className={`status-indicator ${isConnected ? '' : 'disconnected'}`}></span>
          <span>{isConnected ? 'WebSocket Connected' : 'WebSocket Disconnected'}</span>
        </div>
      </div>

      <div className="container">
        <div className="card">
          <h2>Place Order</h2>
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label>Symbol</label>
              <select name="symbol" value={formData.symbol} onChange={handleChange} required>
                <option value="AAPL">AAPL</option>
                <option value="MSFT">MSFT</option>
                <option value="GOOGL">GOOGL</option>
                <option value="AMZN">AMZN</option>
                <option value="TSLA">TSLA</option>
                <option value="META">META</option>
                <option value="NVDA">NVDA</option>
                <option value="NFLX">NFLX</option>
              </select>
            </div>

            <div className="form-group">
              <label>Side</label>
              <select name="side" value={formData.side} onChange={handleChange} required>
                <option value="BUY">BUY</option>
                <option value="SELL">SELL</option>
              </select>
            </div>

            <div className="form-group">
              <label>Quantity</label>
              <input
                type="number"
                name="quantity"
                value={formData.quantity}
                onChange={handleChange}
                min="1"
                required
              />
            </div>

            <div className="form-group">
              <label>Price</label>
              <input
                type="number"
                name="price"
                value={formData.price}
                onChange={handleChange}
                step="0.01"
                min="0.01"
                required
              />
            </div>

            <div className="form-group">
              <label>Order Type</label>
              <select name="orderType" value={formData.orderType} onChange={handleChange} required>
                <option value="LIMIT">LIMIT</option>
                <option value="MARKET">MARKET</option>
              </select>
            </div>

            <button type="submit" className="button" disabled={isSubmitting || !isConnected}>
              {isSubmitting ? 'Submitting...' : 'Submit Order'}
            </button>
          </form>
        </div>

        <div className="card">
          <h2>Market Ticks</h2>
          <div className="ticks-panel">
            <p style={{ color: '#999', fontSize: '14px' }}>
              Market data streaming will be available in a future update.
            </p>
          </div>
        </div>
      </div>

      <div className="card">
        <h2>Event Log</h2>
        <div className="event-log">
          {events.length === 0 ? (
            <p style={{ color: '#858585' }}>No events yet. Submit an order to see events.</p>
          ) : (
            events.map((event, index) => (
              <div key={`${event.eventId}-${index}`} className={`event-entry ${getStatusClass(event.status)}`}>
                <div className="event-time">{formatTimestamp(event.timestamp)}</div>
                <div className="event-details">
                  <strong>Order ID:</strong> {event.orderId} | 
                  <strong> Status:</strong> {event.status} | 
                  <strong> Symbol:</strong> {event.symbol || 'N/A'} | 
                  <strong> Side:</strong> {event.side || 'N/A'} | 
                  <strong> Qty:</strong> {event.quantity || 'N/A'}
                  {event.fillPrice && ` | Fill Price: $${event.fillPrice.toFixed(2)}`}
                </div>
                {event.message && <div style={{ marginTop: '5px', color: '#858585' }}>{event.message}</div>}
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  )
}

export default App

