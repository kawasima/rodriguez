/**
 * Minimal upstream server that simulates a product API.
 * Used as the proxy's upstream target for fault injection tests.
 *
 * Usage: node src/upstream-server.js [port]
 */

import { createServer } from 'node:http';

const PORT = parseInt(process.argv[2] || '8080', 10);

const products = {
  '1':  { id: 1, name: 'Widget',  price: 9.99 },
  '42': { id: 42, name: 'Gadget', price: 19.99 },
};

const server = createServer((req, res) => {
  const match = req.url.match(/^\/products\/(.+)/);
  if (match) {
    const product = products[match[1]];
    if (product) {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify(product));
    } else {
      res.writeHead(404, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Product not found' }));
    }
    return;
  }

  if (req.url === '/products') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(Object.values(products)));
    return;
  }

  res.writeHead(404, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({ error: 'Not found' }));
});

server.listen(PORT, () => {
  console.log(`Upstream server listening on port ${PORT}`);
});

process.on('SIGTERM', () => {
  server.close(() => process.exit(0));
});
