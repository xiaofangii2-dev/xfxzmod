package main

/*
#include <stdlib.h>
*/
import "C"
import (
    "crypto/sha256"
    "encoding/hex"
    "unsafe"
)

//export XfxzEncrypt
func XfxzEncrypt(input *C.char, key *C.char) *C.char {
    in := C.GoString(input)
    k := C.GoString(key)
    if len(k) == 0 {
        k = "xfxz-default"
    }
    out := make([]byte, len(in))
    for i := 0; i < len(in); i++ {
        out[i] = in[i] ^ k[i%len(k)]
    }
    h := sha256.Sum256(out)
    result := hex.EncodeToString(out) + ":" + hex.EncodeToString(h[:])
    return C.CString(result)
}

//export XfxzDecrypt
func XfxzDecrypt(input *C.char, key *C.char) *C.char {
    s := C.GoString(input)
    k := C.GoString(key)
    if len(k) == 0 {
        k = "xfxz-default"
    }
    parts := splitColon(s)
    if parts == nil {
        return C.CString("")
    }
    data, err := hex.DecodeString(parts[0])
    if err != nil {
        return C.CString("")
    }
    out := make([]byte, len(data))
    for i := 0; i < len(data); i++ {
        out[i] = data[i] ^ k[i%len(k)]
    }
    return C.CString(string(out))
}

func splitColon(s string) []string {
    idx := -1
    for i := 0; i < len(s); i++ {
        if s[i] == ':' {
            idx = i
            break
        }
    }
    if idx < 0 {
        return nil
    }
    return []string{s[:idx], s[idx+1:]}
}

//export XfxzFree
func XfxzFree(p *C.char) {
    C.free(unsafe.Pointer(p))
}

func main() {}
