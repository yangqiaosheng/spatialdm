#! /bin/bash

RED="\e[0;31m"
nc="\e[0m"

tmpDir="hdfs://lee.informatik.rwth-aachen.de:9000/user/hadoop/group2/tmp_out"

#initial points
#inputCenters="hdfs://lee.informatik.rwth-aachen.de:9000/user/hadoop/group2/test_data/testCentersEx3.csv"
inputCenters="hdfs://lee.informatik.rwth-aachen.de:9000/user/hadoop/data/exercise3/centers.csv"
outputCenters="hdfs://lee.informatik.rwth-aachen.de:9000/user/hadoop/group2/kmeans_out/finalCenters.csv"

outputCentersFileLocal="newCenters.csv"
outputCentersFileLocalOld="oldCenters.csv"
iteration=1

echo -e "${RED}Creating outputOld stub for the first comparison${nc}"
cat /dev/null > $outputCentersFileLocalOld

echo -e "${RED}Removing old centers output file${nc}"
/home/hadoop/shrt_hadoop.sh fs -rm $outputCenters

while true; do
	echo -e "${RED}Deleting logs${nc}"
	~/deleteLogs.sh 1> /dev/null #ignore normal output

	echo -e "${RED}Cleaning up tmp dir${nc}"
	/home/hadoop/shrt_hadoop.sh fs -rmr $tmpDir

	echo -e "${RED}Running hadoop job${nc}"
	/home/hadoop/shrt_hadoop.sh jar /home/hadoop/group2/upload/HadoopEx3.jar ${group2}.JobExecute $iteration $inputCenters

    # remove old local file, so getmerge does not complain
	echo -e "${RED}Getting merge of iteration ${iteration}${nc}"
    rm -f $outputCentersFileLocal
	# fick dich getmerge
	/home/hadoop/shrt_hadoop.sh fs -cat $tmpDir/*.csv > $outputCentersFileLocal
	#/home/hadoop/shrt_hadoop.sh fs -getmerge $tmpDir/*.csv $outputCentersFileLocal
				
	# sort the output
	echo -e "${RED}Sorting new output${nc}"
	sort -o $outputCentersFileLocal $outputCentersFileLocal

	echo -e "${RED}Diff - will continue if there are differences${nc}"
	change=`diff $outputCentersFileLocal $outputCentersFileLocalOld`
	
    # if output was an empty string (which means no difference)
	if [ ! -n "$change" ]; then
		echo -e "${RED}No change! My work here is done.${nc}"
		break
	fi
				
	# use output as old output for next iteration
	echo -e "${RED}Overwriting old centers with new ones${nc}"
	cat $outputCentersFileLocal > $outputCentersFileLocalOld
	
	# upload sorted output
	echo -e "${RED}Uploading sorted output to hdfs - fails the first time${nc}"
    # deleting old file first
	/home/hadoop/shrt_hadoop.sh fs -rm $outputCenters
	/home/hadoop/shrt_hadoop.sh fs -copyFromLocal $outputCentersFileLocalOld $outputCenters
							
	# only needed after first iteration
	inputCenters=$outputCenters
	#iterations++
	iteration=$(($iteration+1))
done
				
echo -e "${RED}Done after $iteration iterations. Success.${nc}"

#cleanup	
echo -e "${RED}Removing old local file${nc}"
rm -f $outputCentersFileLocalOld

