const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 3030;
const FILE_PATH = path.join(__dirname, 'index.html');

const server = http.createServer((req, res) => {
  if (req.url === '/' || req.url === '/index.html') {
    fs.readFile(FILE_PATH, (err, content) => {
      if (err) {
        res.writeHead(500, { 'Content-Type': 'text/plain; charset=utf-8' });
        res.end('Error loading simulator index.html');
      } else {
        res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
        res.end(content);
      }
    });
  } else {
    res.writeHead(404, { 'Content-Type': 'text/plain' });
    res.end('Not Found');
  }
});

server.listen(PORT, () => {
  console.log(`\n======================================================`);
  console.log(`🚀 Easy Payment - Android Simulation Server is running!`);
  console.log(`📱 Local URL: http://localhost:${PORT}`);
  console.log(`💡 To view inside Antigravity IDE:`);
  console.log(`   Press Ctrl+Shift+P -> Search 'Simple Browser: Show' -> Enter http://localhost:${PORT}`);
  console.log(`======================================================\n`);
});
