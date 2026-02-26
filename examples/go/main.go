package main

import (
	"fmt"
	"os"
	"path/filepath"
)

func main() {
	mountPath := os.Getenv("RODRIGUEZ_FUSE_MOUNT")
	if mountPath == "" {
		mountPath = "/tmp/rodriguez-fuse"
	}

	fmt.Printf("FUSE mount path: %s\n", mountPath)

	// Test 1: Write a file (may fail with ENOSPC if DiskFull fault is configured)
	testFile := filepath.Join(mountPath, "test.log")
	fmt.Printf("\n[1] Writing to %s\n", testFile)
	err := os.WriteFile(testFile, []byte("Hello from Go\n"), 0644)
	if err != nil {
		fmt.Printf("    FAULT: %v\n", err)
	} else {
		fmt.Println("    OK: Write succeeded")
	}

	// Test 2: Read the file back
	fmt.Printf("\n[2] Reading from %s\n", testFile)
	data, err := os.ReadFile(testFile)
	if err != nil {
		fmt.Printf("    FAULT: %v\n", err)
	} else {
		fmt.Printf("    OK: Read %q\n", string(data))
	}

	// Test 3: Create a directory
	testDir := filepath.Join(mountPath, "subdir")
	fmt.Printf("\n[3] Creating directory %s\n", testDir)
	err = os.MkdirAll(testDir, 0755)
	if err != nil {
		fmt.Printf("    FAULT: %v\n", err)
	} else {
		fmt.Println("    OK: Directory created")
	}

	// Test 4: List files
	fmt.Printf("\n[4] Listing %s\n", mountPath)
	entries, err := os.ReadDir(mountPath)
	if err != nil {
		fmt.Printf("    FAULT: %v\n", err)
	} else {
		names := make([]string, len(entries))
		for i, e := range entries {
			names[i] = e.Name()
		}
		fmt.Printf("    OK: %v\n", names)
	}

	// Test 5: Delete the file
	fmt.Printf("\n[5] Deleting %s\n", testFile)
	err = os.Remove(testFile)
	if err != nil {
		fmt.Printf("    FAULT: %v\n", err)
	} else {
		fmt.Println("    OK: File deleted")
	}

	// Cleanup
	_ = os.Remove(testDir)
}
