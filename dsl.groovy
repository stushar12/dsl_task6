  
job("devops_task6_job1"){
  description("Pull the data from github repo automatically when some developers push code to github")
  scm{
    github("stushar12/task6_devops","master")
  }
  triggers {
    scm("* * * * *")
  }
  steps{
    shell('''if ls / | grep task6_devops
then
sudo rm -rf /task6_devops
else
sudo mkdir /task6_devops
sudo cp -rvf * /task6_devops
fi
''')
  }
}

job("devops_task6_job2"){
  description("By looking at the code it will launch the deployment of respective webserver and the deployment will launch webserver, create PVC and expose the deployment")

  triggers {
    upstream("devops_task6_job1", "SUCCESS")
  }
  steps{
    shell('''data=$(sudo ls /task6_devops)
if  sudo ls /task6_devops/ | grep '[a-z].php'
then
echo "It is a php code"

if sudo kubectl get deploy | grep webserver
then
echo "php container is running"
else
sudo kubectl create -f /task6_devops/deploy.yml
fi
else
echo "No php code found"
fi
''')
  }
}


job("devops_task6_job3"){
  description("Check whether the web app is working or not. If it is not working sent email to developer")
  triggers {
    upstream("devops_task6_job2", "SUCCESS")
  }
  steps{
    shell('''if sudo ls /task6_devops | grep html
then
status=$(sudo curl -o /dev/null -s -w "%{http_code}" 192.168.99.101:30100)
elif sudo ls /task6_devops | grep php
then
status=$(sudo curl -o /dev/null -s -w "%{http_code}" 192.168.99.101:30100)
fi
if [[ status -ne 200 ]]
then 
sudo python3 mail.py
else
echo "Code is fine"
fi
''')
  }
}

job("devops_task6_job4"){
  description("For monitoring of the container and to launch another if the existing fails.")

  triggers {
    upstream("devops_task6_job3", "SUCCESS")
  }
shell('''if sudo kubectl get deployment | grep webserver
then
exit 0
else
sudo kubectl create -f /task6_devops/deploy.yml
sleep 10
fi
''')
}



buildPipelineView("Pipeline view") {
  filterBuildQueue(true)
  filterExecutors(false)
  title("My pipeline")
  displayedBuilds(1)
  selectedJob("devops_task6_job1")
  alwaysAllowManualTrigger(true)
  showPipelineParameters(true)
  refreshFrequency(10)
}
