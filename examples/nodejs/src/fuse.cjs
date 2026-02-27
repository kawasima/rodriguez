const fs = require('fs');
const path = require('path');

const mountPath = process.env.RODRIGUEZ_FUSE_MOUNT || '/tmp/rodriguez-fuse';

console.log(`FUSE mount path: ${mountPath}`);

// Test 1: Write a file (may fail with ENOSPC if DiskFull fault is configured)
const testFile = path.join(mountPath, 'test.log');
console.log(`\n[1] Writing to ${testFile}`);
try {
  fs.writeFileSync(testFile, 'Hello from Node.js\n');
  console.log('    OK: Write succeeded');
} catch (err) {
  console.log(`    FAULT: ${err.code} - ${err.message}`);
}

// Test 2: Read the file back (may fail with EIO or return corrupted data)
console.log(`\n[2] Reading from ${testFile}`);
try {
  const data = fs.readFileSync(testFile, 'utf8');
  console.log(`    OK: Read "${data.trim()}"`);
} catch (err) {
  console.log(`    FAULT: ${err.code} - ${err.message}`);
}

// Test 3: Create a directory
const testDir = path.join(mountPath, 'subdir');
console.log(`\n[3] Creating directory ${testDir}`);
try {
  fs.mkdirSync(testDir, { recursive: true });
  console.log('    OK: Directory created');
} catch (err) {
  console.log(`    FAULT: ${err.code} - ${err.message}`);
}

// Test 4: List files
console.log(`\n[4] Listing ${mountPath}`);
try {
  const files = fs.readdirSync(mountPath);
  console.log(`    OK: ${files.join(', ')}`);
} catch (err) {
  console.log(`    FAULT: ${err.code} - ${err.message}`);
}

// Test 5: Delete the file
console.log(`\n[5] Deleting ${testFile}`);
try {
  fs.unlinkSync(testFile);
  console.log('    OK: File deleted');
} catch (err) {
  console.log(`    FAULT: ${err.code} - ${err.message}`);
}

// Cleanup
try { fs.rmdirSync(testDir); } catch (_) {}
