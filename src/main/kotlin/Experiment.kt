import experiments.PlacesExperiment
import experiments.ToyExperiment

fun main(args: Array<String>) {
    val experimentName = args.getOrElse(0) { "toy" }
    when (experimentName) {
        "places" -> {
            val inputPath = args.getOrNull(1)
            val outputPath = args.getOrElse(2) { "results/output_places_US.csv" }

            if (inputPath == null) {
                println("Path to Places dataset should be provided as an argument.")
                return
            }

            val experiment = PlacesExperiment(outputPath, inputPath, 1000000,5000000, 0.01)
            experiment.run()
        }
        "toy" -> {
            val outputPath = args.getOrNull(1)
            val experiment = ToyExperiment(outputPath)
            experiment.run()
        }
    }
}