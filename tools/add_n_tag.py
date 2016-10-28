f = open(raw_input("Type input file name: "), 'r')
s = open(raw_input("Type output file name: "), 'w')
print "\n"

counter = 0

for line in f:
    line = line.strip('\n')
    conv = "N" + str(counter) + " " + line + "\n"
    counter += 1
    print conv
    s.write(conv)

f.close()
s.close()
