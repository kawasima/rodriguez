# rodriguez-fuse

Rodriguez supports file I/O fault injection using FUSE (Filesystem in Userspace).

Unlike network or JDBC harnesses, this module works at the OS level — any application in any language
that reads/writes files through the mounted path will experience the configured faults.

## Prerequisites

FUSE libraries must be installed on your system:

| Platform | Installation |
| --- | --- |
| macOS | `brew install --cask macfuse` |
| Linux (Debian/Ubuntu) | `sudo apt-get install libfuse-dev` |
| Linux (RHEL/CentOS) | `sudo yum install fuse-devel` |
| Windows | Install [WinFsp](https://winfsp.dev/) |

## How it works

```
Application writes to /tmp/rodriguez-fuse/data.txt
       │
       ▼ (FUSE kernel driver)
FaultInjectionFS (passthrough + fault injection)
       │
       ├─ Normal: delegates to backing path /tmp/real-data/data.txt
       └─ Fault:  returns errno (ENOSPC, EIO, ...) / delays / corrupts data
```

## Configuration

```json
{
  "mountPath": "/tmp/rodriguez-fuse",
  "backingPath": "/tmp/real-data",
  "faults": [
    {
      "pathPattern": ".*\\.log$",
      "operations": ["WRITE", "FSYNC"],
      "fault": {
        "type": "DiskFull"
      }
    },
    {
      "pathPattern": ".*",
      "operations": ["READ"],
      "fault": {
        "type": "SlowIO",
        "delayMs": 3000
      }
    }
  ]
}
```

## Usage

### Standalone

```
java -cp rodriguez-fuse.jar net.unit8.rodriguez.fuse.FuseHarnessCommand -c fuse-config.json
```

### Programmatic

```java
FuseConfig config = new FuseConfig();
config.setMountPath("/tmp/rodriguez-fuse");
config.setBackingPath("/tmp/real-data");
config.setFaults(List.of(
    new FaultRule(".*\\.log$", Set.of(FuseOperation.WRITE), new DiskFull())
));

FuseHarness harness = new FuseHarness(config);
harness.start();

// ... run your tests ...

harness.shutdown();
```

### Testing with any language

Once mounted, the fault injection works transparently for any application:

```bash
# Shell
echo "data" > /tmp/rodriguez-fuse/output.log
# → write(1, "data\n", 5): No space left on device

# Python
with open("/tmp/rodriguez-fuse/output.log", "w") as f:
    f.write("data")  # → OSError: [Errno 28] No space left on device

# Go
os.WriteFile("/tmp/rodriguez-fuse/output.log", data, 0644)
// → write /tmp/rodriguez-fuse/output.log: no space left on device

# Node.js
fs.writeFileSync("/tmp/rodriguez-fuse/output.log", "data");
// → Error: ENOSPC: no space left on device
```

## Fault patterns

### DiskFull

Returns `ENOSPC` (No space left on device).

### PermissionDenied

Returns `EACCES` (Permission denied).

### ReadOnlyFS

Returns `EROFS` (Read-only file system).

### IOError

Returns `EIO` (Input/output error).

### FileNotFound

Returns `ENOENT` (No such file or directory).

### TooManyOpenFiles

Returns `EMFILE` (Too many open files).

### SlowIO

Delays the operation by a configurable duration, then returns the normal result.

| property | description | default |
| --- | --- | --- |
| delayMs | Delay in milliseconds | 5000 |

### CorruptedRead

Returns data with random bytes injected.

| property | description | default |
| --- | --- | --- |
| corruptionRate | Probability of corruption per read (0.0-1.0) | 0.1 |

### PartialWrite

Writes only a fraction of the requested bytes.

| property | description | default |
| --- | --- | --- |
| writeRatio | Fraction of bytes actually written (0.0-1.0) | 0.5 |

## Fault rules

Each fault rule has three components:

| field | description |
| --- | --- |
| pathPattern | Regex pattern matched against the file path (null = all paths) |
| operations | Set of `FuseOperation` values to match (null = all operations) |
| fault | The fault to apply when matched |

Available operations: `READ`, `WRITE`, `OPEN`, `CREATE`, `TRUNCATE`, `FSYNC`, `FLUSH`, `MKDIR`, `UNLINK`, `RMDIR`, `RENAME`, `CHMOD`, `CHOWN`

Rules are evaluated in order; the first matching rule wins.

## Docker

```
docker run -it --rm --privileged \
  -v ./fuse-config.json:/app/fuse-config.json \
  kawasima/rodriguez-fuse -c /app/fuse-config.json
```

Note: `--privileged` is required for FUSE mount operations inside a container.
