import SwiftUI
import SinusApproximatorKit

struct ContentView: View {
    @State var radians = 0.0
    @State private var showContent = false

    let sineCalc = ASinusCalculator {
        guard let dataURL = Bundle.main.url(forResource: "sinus", withExtension: "json") else {
            fatalError("Could not load model")
        }
        let data = try! Data(contentsOf: dataURL)
        let buffer = Kotlinx_io_coreBuffer()
        buffer.write(source: data.kotlinByteArray, startIndex: 0, endIndex: Int32(data.count))
        return buffer
    }
    
    var body: some View {
        VStack {
            VStack {
                Slider(value: $radians, in: 0.0...Double.pi/2) {
                    
                }
                Text("Winkel: \(radians, format: .number.precision(.fractionLength(2))) rad")
            }
            .padding()
            let sin = sin(radians)
            Text("Sinus: \(sin, format: .number.precision(.fractionLength(4)))")
                .font(.title)
            let modelSin = sineCalc.calculate(x: radians)
            Text("Model Sinus: \(modelSin, format: .number.precision(.fractionLength(4)))")
            Button("Modell Laden") {
                Task {
                    do {
                        try await sineCalc.loadModel()
                    } catch {
                        print("Error loading model: \(error)")
                    }
                }
            }
            .padding()
          }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .padding()
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
