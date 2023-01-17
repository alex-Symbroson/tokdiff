
# Tokdiff

Tokdiff is a tool for comparing tokenized text files and generating reports of the differences. It takes as input a directory containing text files, as well as corresponding tokenized versions of the files in the form of zip files. The tool then calculates the differences between the tokenized texts and compares them with the original text.

## Execution

To run the tool, you can use the following command:

```sh
./gradlew run --args="[base directory] [max file index] [write diffs] [filter category]"
```

- `base directory`: The directory where the text files and zip files are located. If no directory is specified, the current working directory will be used.
- `max file index`: The maximum number of files to process. If not specified, all files will be processed.
- `write diffs`: A boolean value indicating whether to write the differences to an excel file. The default value is true.
- `filter category`: The category to filter differences by. If not specified, all differences will be included.

## Input

The input files for this tool are zip files for each tokenizer including the tokenized texts. The tool then compares each tokenized versions of the text, and generates an excel file that contains the differences across all tokenizers.

Optionally a `input.zip` file is scanned for the original versions of the text. This enables information about the sentence context and the original location in the text file.\
It is expected that each `.txt` file has a matching tokenized `.tok` version. The zip files should be named in a way that it is clear which tokenizer was used, for example "NLTK_nist.zip" or "spaCy.zip".

## Output

The tool generates a `diffs-X.xlsx` file located in the specified root directory that contains the differences between the tokenized versions and the original text. This excel file will have a sheet for each processed text file and contains the following columns:

- `File name`: name of the processed file
- `Position`: the position of the difference in the original text
- `Context`: the context of the difference in the original text
- `Category`: the category of difference (e.g. Number, Punctuation)
- `Original Tokens`: the original text tokens
- `Tokenized Tokens`: the tokenized text tokens
- One column for each tokenized file, showing the corresponding tokenized text.

Additionally, The tool also generates a `summary.xlsx` file with a chart showing the distribution of the differences by category.\
Note: If the writeDiffs option is set to false no output files will be generated.