#!/usr/bin/env node

/**
 * Latency measurement script for order processing
 * Measures latency from order submission to order acceptance/fill via WebSocket
 * 
 * Usage: node measure-latency.js [number_of_orders] [order_api_url] [ws_url]
 */

// Note: Install dependencies with: npm install axios sockjs-client stompjs
// Or run: npm install --prefix scripts axios sockjs-client stompjs

let axios, SockJS, Client;
try {
    axios = require('axios');
    SockJS = require('sockjs-client');
    const stompjs = require('stompjs');
    Client = stompjs.Client || stompjs;
} catch (error) {
    console.error('Missing dependencies. Please install:');
    console.error('  npm install axios sockjs-client stompjs');
    process.exit(1);
}

const ORDER_API_URL = process.argv[3] || 'http://localhost:8080';
const WS_URL = process.argv[4] || 'http://localhost:8080';
const NUM_ORDERS = parseInt(process.argv[2]) || 10;

const SYMBOLS = ['AAPL', 'MSFT', 'GOOGL', 'AMZN', 'TSLA', 'META', 'NVDA', 'NFLX'];
const SIDES = ['BUY', 'SELL'];
const ORDER_TYPES = ['LIMIT', 'MARKET'];

// Track order submissions and events
const orderTimestamps = new Map();
const orderEvents = new Map();
let ordersSubmitted = 0;
let ordersCompleted = 0;

// Statistics
const latencies = {
    accepted: [],
    filled: []
};

function generateOrder() {
    const symbol = SYMBOLS[Math.floor(Math.random() * SYMBOLS.length)];
    const side = SIDES[Math.floor(Math.random() * SIDES.length)];
    const quantity = Math.floor(Math.random() * 1000) + 1;
    const price = (Math.random() * 500 + 50).toFixed(2);
    const orderType = ORDER_TYPES[Math.floor(Math.random() * ORDER_TYPES.length)];
    
    return {
        symbol,
        side,
        quantity,
        price: parseFloat(price),
        orderType
    };
}

function calculatePercentiles(values) {
    if (values.length === 0) return { p50: 0, p90: 0, p95: 0, p99: 0 };
    
    const sorted = [...values].sort((a, b) => a - b);
    const p50 = sorted[Math.floor(sorted.length * 0.5)];
    const p90 = sorted[Math.floor(sorted.length * 0.9)];
    const p95 = sorted[Math.floor(sorted.length * 0.95)];
    const p99 = sorted[Math.floor(sorted.length * 0.99)];
    
    return { p50, p90, p95, p99 };
}

function printStatistics() {
    console.log('\n==========================================');
    console.log('Latency Measurement Summary');
    console.log('==========================================');
    console.log(`Total orders submitted: ${ordersSubmitted}`);
    console.log(`Orders with ACCEPTED events: ${latencies.accepted.length}`);
    console.log(`Orders with FILLED events: ${latencies.filled.length}`);
    console.log('');
    
    if (latencies.accepted.length > 0) {
        const acceptedStats = calculatePercentiles(latencies.accepted);
        const avgAccepted = latencies.accepted.reduce((a, b) => a + b, 0) / latencies.accepted.length;
        const minAccepted = Math.min(...latencies.accepted);
        const maxAccepted = Math.max(...latencies.accepted);
        
        console.log('ACCEPTED Event Latency (ms):');
        console.log(`  Min:    ${minAccepted.toFixed(2)}ms`);
        console.log(`  Avg:    ${avgAccepted.toFixed(2)}ms`);
        console.log(`  P50:    ${acceptedStats.p50.toFixed(2)}ms`);
        console.log(`  P90:    ${acceptedStats.p90.toFixed(2)}ms`);
        console.log(`  P95:    ${acceptedStats.p95.toFixed(2)}ms`);
        console.log(`  P99:    ${acceptedStats.p99.toFixed(2)}ms`);
        console.log(`  Max:    ${maxAccepted.toFixed(2)}ms`);
        console.log('');
    }
    
    if (latencies.filled.length > 0) {
        const filledStats = calculatePercentiles(latencies.filled);
        const avgFilled = latencies.filled.reduce((a, b) => a + b, 0) / latencies.filled.length;
        const minFilled = Math.min(...latencies.filled);
        const maxFilled = Math.max(...latencies.filled);
        
        console.log('FILLED Event Latency (ms):');
        console.log(`  Min:    ${minFilled.toFixed(2)}ms`);
        console.log(`  Avg:    ${avgFilled.toFixed(2)}ms`);
        console.log(`  P50:    ${filledStats.p50.toFixed(2)}ms`);
        console.log(`  P90:    ${filledStats.p90.toFixed(2)}ms`);
        console.log(`  P95:    ${filledStats.p95.toFixed(2)}ms`);
        console.log(`  P99:    ${filledStats.p99.toFixed(2)}ms`);
        console.log(`  Max:    ${maxFilled.toFixed(2)}ms`);
    }
    
    console.log('==========================================\n');
}

async function submitOrder(orderData) {
    try {
        const submitTime = Date.now();
        const response = await axios.post(`${ORDER_API_URL}/api/v1/orders`, orderData);
        const orderId = response.data.orderId;
        
        orderTimestamps.set(orderId, {
            submitted: submitTime,
            orderData
        });
        ordersSubmitted++;
        
        console.log(`[${ordersSubmitted}/${NUM_ORDERS}] Order submitted: ${orderId}`);
        return orderId;
    } catch (error) {
        console.error(`Error submitting order:`, error.message);
        return null;
    }
}

function setupWebSocket() {
    return new Promise((resolve, reject) => {
        const socket = new SockJS(`${WS_URL}/ws/orders`);
        const client = Client.over(socket);
        
        client.connect({}, () => {
            console.log('WebSocket connected');
            
            client.subscribe('/topic/orders', (message) => {
                try {
                    const event = JSON.parse(message.body);
                    handleOrderEvent(event);
                } catch (error) {
                    console.error('Error parsing event:', error);
                }
            });
            
            resolve(client);
        }, (error) => {
            console.error('WebSocket connection error:', error);
            reject(error);
        });
    });
}

function handleOrderEvent(event) {
    const orderId = event.orderId;
    const timestamp = orderTimestamps.get(orderId);
    
    if (!timestamp) {
        return; // Order not tracked
    }
    
    const eventTime = new Date(event.timestamp).getTime();
    const latency = eventTime - timestamp.submitted;
    
    if (event.status === 'ACCEPTED') {
        if (!orderEvents.has(orderId) || orderEvents.get(orderId).status !== 'ACCEPTED') {
            latencies.accepted.push(latency);
            orderEvents.set(orderId, { ...event, latency });
            console.log(`  ✓ ACCEPTED: ${orderId} (latency: ${latency.toFixed(2)}ms)`);
        }
    } else if (event.status === 'FILLED') {
        if (!orderEvents.has(orderId) || orderEvents.get(orderId).status !== 'FILLED') {
            latencies.filled.push(latency);
            orderEvents.set(orderId, { ...event, latency });
            ordersCompleted++;
            console.log(`  ✓ FILLED: ${orderId} (latency: ${latency.toFixed(2)}ms)`);
            
            // Check if all orders are completed
            if (ordersCompleted >= NUM_ORDERS) {
                setTimeout(() => {
                    printStatistics();
                    process.exit(0);
                }, 2000); // Wait 2 seconds for any remaining events
            }
        }
    }
}

async function main() {
    console.log('==========================================');
    console.log('Order Processing Latency Measurement');
    console.log('==========================================');
    console.log(`Order API: ${ORDER_API_URL}`);
    console.log(`WebSocket: ${WS_URL}`);
    console.log(`Number of orders: ${NUM_ORDERS}`);
    console.log('==========================================\n');
    
    // Setup WebSocket connection
    let client;
    try {
        client = await setupWebSocket();
    } catch (error) {
        console.error('Failed to connect WebSocket:', error);
        process.exit(1);
    }
    
    // Wait a bit for WebSocket to be ready
    await new Promise(resolve => setTimeout(resolve, 1000));
    
    // Submit orders
    const promises = [];
    for (let i = 0; i < NUM_ORDERS; i++) {
        const order = generateOrder();
        promises.push(submitOrder(order));
        
        // Small delay between submissions
        await new Promise(resolve => setTimeout(resolve, 50));
    }
    
    await Promise.all(promises);
    
    // Wait for all events or timeout after 60 seconds
    const timeout = setTimeout(() => {
        console.log('\nTimeout reached. Printing statistics...');
        printStatistics();
        if (client) client.disconnect();
        process.exit(0);
    }, 60000);
    
    // Clear timeout if all orders complete
    const checkInterval = setInterval(() => {
        if (ordersCompleted >= NUM_ORDERS) {
            clearTimeout(timeout);
            clearInterval(checkInterval);
        }
    }, 1000);
}

main().catch(error => {
    console.error('Error:', error);
    process.exit(1);
});

