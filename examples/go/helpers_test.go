package main

import "fmt"

const (
	portSlowResponse        = 10205
	portResponseHeaderOnly  = 10207
	portAcceptButSilent     = 10209
)

func endpointURL(port int) string {
	return fmt.Sprintf("http://localhost:%d", port)
}
