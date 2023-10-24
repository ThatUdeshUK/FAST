package exceptions

class InvalidOutputFile(path: String): Exception("Invalid output file: $path!")