import re, time, sys

def main():
    file_name = raw_input("enter file name: ")

    try:
        ext_index = file_name.index(".")
        ext = file_name[ext_index:]
        file_conv_name = file_name[:ext_index] + "_converted" + ext
    except:
        file_conv_name = file_name + "_converted.txt"

    try:
        file_input = open(file_name, 'r')
        file_output = open(file_conv_name, 'w')
    except:
        print "file error"
        return -1

    print "Converting " + file_name + " ..."

    last_x = 0.0
    last_y = 0.0
    cur_x = 0.0
    cur_y = 0.0

    for line in file_input:
        line = line.strip("\n")
        line = line.replace("\t", " ")
        conv_line = ""

        try:
            comment_index = line.index("(")
            if comment_index != 0:
                base = line[:comment_index]
                comment = line[comment_index:]
                line = base + " " + comment
        except:
            pass

        try:
            comment_index = line.index(")")
            base = line[comment_index+1:]
            comment = line[:comment_index+1]
            line = comment + " " + base
        except:
            pass

        for block in line.split(" "):

            xy_matcher = re.match("[XY]([-])?((\d+\.\d+)|(\d+)|(\.\d+))\Z", block)
            ij_matcher = re.match("[IJ]([-])?((\d+\.\d+)|(\d+)|(\.\d+))\Z", block)

            if ij_matcher != None:
                if block[0] == "I":
                    block = "I" + str(float(block[1:]) - last_x)
                elif block[0] == "J":
                    block = "J" + str(float(block[1:]) - last_y)

            if xy_matcher != None:
                if block[0] == "X":
                    cur_x = float(block[1:])
                elif block[0] == "Y":
                    cur_y = float(block[1:])


            conv_line += block + " "

        last_x = cur_x
        last_y = cur_y

        file_output.write(conv_line + "\n")

    file_input.close()
    file_output.close()

    return 0

if __name__ == "__main__":
    while True:
        start_time = time.time()
        status = main()
        if status == 0:
            print "Converted in " + str(round(time.time() - start_time, 2)) + "ms"
        if raw_input("type 1 to restart, press enter to exit: ") == "1":
            continue
        sys.exit(0)
