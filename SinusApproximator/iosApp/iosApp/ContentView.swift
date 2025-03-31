import SwiftUI
import SinusApproximatorKit

struct ContentView: View {
    @State var radians = 0.0
    @State private var showContent = false
    let kIx = Kotlinx_io_coreBuffer()
    let y:Kotlinx_io_coreSource? = nil
    let sineCalc = ASinusCalculator {
        guard let dataURL = Bundle.main.url(forResource: "sinus", withExtension: "json"), let input = InputStream(url: dataURL) else {
            fatalError("Could not load model")
        }
        return KotlinInputStream(input)
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
                // try await sineCalc.loadModel()
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
