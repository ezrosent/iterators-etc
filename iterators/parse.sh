# -v : search for lines that do not contain the pattern
# -F : treat the expressionas literal string
# -E : specify multiple expressions as ORed expressions
# ^: lines starting with a character

grep -vwF '(65536,32768)' output.txt | grep -vwF '(4096,2048)' | grep -Ev '^2|^4|^6' > parse_updater_output.txt
grep -vwF '(65536,32768)' output.txt | grep -vwF '(16384,8192)' | grep -Ev '^2|^4|^6' > parse_updater_com_output.txt
grep -vwF '(65536,32768)' output.txt | grep -vwF '(4096,2048)' | grep -v '	9	' > parse_iterator_output.txt
grep -vwF '(65536,32768)' output.txt | grep -vwF '(16384,8192)' | grep -v '	9	' > parse_iterator_com_output.txt

