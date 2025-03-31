//
//  kotlinxio.swift
//  iosApp
//
//  Created by Alexander von Below on 30.03.25.
//  Copyright © 2025 Alexander von Below. All rights reserved.
//

import Foundation
import SinusApproximatorKit

extension Data {
    var kotlinByteArray: KotlinByteArray {
        let byteArray = KotlinByteArray(size: Int32(self.count)) { (i: KotlinInt) in
            return KotlinByte(char: CChar(self[i.intValue]))
        }
        return byteArray
    }
}

extension NSMutableData: @retroactive Kotlinx_io_coreRawSink {
    public func flush() {
        self.flush()
    }
    
    public func write(source: Kotlinx_io_coreBuffer, byteCount: Int64) {
        while !source.exhausted() {
            var byte = source.readByte()
            self.append(&byte, length: 1)
        }
    }
    
    public func close() {
        return
    }
}

extension String {
    var byteArray: KotlinByteArray {
        let data = self.data(using: .utf8)!
        return data.kotlinByteArray
    }
}

class KotlinInputStream: Kotlinx_io_coreSource {
    private var inputStream: InputStream
    
    init(_ inputStream: InputStream) {
        self.inputStream = inputStream
        self.inputStream.open()
        self.buffer = Kotlinx_io_coreBuffer()
    }
    
    func exhausted() -> Bool {
        return !inputStream.hasBytesAvailable
    }
    
    func peek() -> any Kotlinx_io_coreSource {
        return self
    }
    
    func readAtMostTo(sink: KotlinByteArray, startIndex: Int32, endIndex: Int32) -> Int32 {
        fatalError("Not implemented")
    }
    
    func readByte() -> Int8 {
        fatalError("Not implemented")
    }
    
    func readInt() -> Int32 {
        fatalError("Not implemented")
    }
    
    func readLong() -> Int64 {
        fatalError("Not implemented")
    }
    
    func readShort() -> Int16 {
        fatalError("Not implemented")
    }
    
    func readTo(sink: any Kotlinx_io_coreRawSink, byteCount: Int64) {
        var data = Data(count: Int(byteCount))
        var read = 0
        withUnsafeMutableBytes(of: &data) { (ptr: UnsafeMutableRawBufferPointer) in
            guard let bM: UnsafeMutablePointer<UInt8> = ptr.bindMemory(to: UInt8.self).baseAddress else {
                return
            }
            read = inputStream.read(bM, maxLength: Int(byteCount))
        }
        let byteArray = data.kotlinByteArray
        let buffer = Kotlinx_io_coreBuffer()
        buffer.write(source: byteArray, startIndex: 0, endIndex: Int32(read))
        sink.write(source: buffer, byteCount: Int64(read))
    }
    
    func request(byteCount: Int64) -> Bool {
        fatalError("Not implemented")
    }
    
    func require(byteCount: Int64) {
        fatalError("Not implemented")
    }
    
    func skip(byteCount: Int64) {
        fatalError("Not implemented")
    }
    
    func transferTo(sink: any Kotlinx_io_coreRawSink) -> Int64 {
        fatalError("Not implemented")
    }
    
    var buffer: Kotlinx_io_coreBuffer
    
    func readAtMostTo(sink: Kotlinx_io_coreBuffer, byteCount: Int64) -> Int64 {
        fatalError("Not implemented")
    }
    
    func close() {
        self.inputStream.close()
    }
}
