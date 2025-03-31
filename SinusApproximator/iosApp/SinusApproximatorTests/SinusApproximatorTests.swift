//
//  SinusApproximatorTests.swift
//  SinusApproximatorTests
//
//  Created by Alexander von Below on 30.03.25.
//  Copyright © 2025 orgName. All rights reserved.
//

import Testing
import Foundation
import SinusApproximatorKit

struct SinusApproximatorTests {

    @Test func byteArrayTest() async throws {
        let byteArray = "Hello, World!".byteArray
        let h: Int8 = byteArray.get(index: 0)
        #expect(byteArray.size == 13)
        #expect(h == 72)
    }
    
    @Test func simpleBufferTest() async throws {
        let buffer = Kotlinx_io_coreBuffer()
        buffer.writeByte(byte: 72)
        #expect(buffer.size == 1)
    }
    
    @Test func bufferWriteTest() async throws {
        let buffer = Kotlinx_io_coreBuffer()
        let byteArray = "Hello, World!".byteArray
        buffer.write(source: byteArray, startIndex: 0, endIndex: 13)
        #expect(buffer.size == 13)
    }
    
    @Test func inputStreamTest() async throws {
        let inputStream = InputStream(data: "Hello, World!".data(using: .utf8)!)
        let kotlinInputStream = KotlinInputStream(inputStream)
        let sink = NSMutableData()
        kotlinInputStream.readTo(sink: sink, byteCount: 5)
        
        let bytes = sink.bytes.assumingMemoryBound(to: UInt8.self)
        let x = bytes[0]
        #expect(x == 72)
    }
}
