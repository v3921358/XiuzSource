/**
*     
*@author XoticStory
*
*/
//var status;
//var jobs = [new Array(), new Array(), new Array(), new Array(), new Array()]; // shit load of empty arrays.
//var jobSelArray;

function start() {
	cm.sendNext("Sorry, this feature is not available no more");
	cm.dispose();
/*
    status = -1;
    cm.gainItem(2022065, -1);
    cm.sendYesNo("Hi #r#h ##k,\r\nDo you want to switch back to a previous job ?\r\n\t\t\t\t\t\t\t\t#fEffect/ItemEff/1112002/1/6#");
*/
}

function action(Job, Changer, FTW) {
    if (Job == 1) {
        status++;
    } else {
        if (Changer == 1 && Job == 0)
            cm.sendOk("Ok then, see you later");
        cm.gainItem(2022065, 1, false);
        cm.dispose();
        return;
    }
	
    var pastJobs = cm.getPlayer().getJobs();
    if (status == 1) {
        if (pastJobs.size() == 0) {
            cm.sendOk("You haven't had any past jobs before ! Try Rebirthing to use this!");
            cm.gainItem(2022065, 1, false);
            cm.dispose();
        } else {
            for (var i = 0; i < pastJobs.size(); i++)
                jobs[Math.floor(pastJobs.get(i) / 100 - 1)].push(pastJobs.get(i));
            var text = "Choose You Job Category\r\n#b";
            if (jobs[0].length > 0)
                text += "\r\n#L0#Warrior#l";
            if (jobs[1].length > 0)
                text += "\r\n#L1#Magician#l";
            if (jobs[2].length > 0)
                text += "\r\n#L2#Bowman#l";
            if (jobs[3].length > 0)
                text += "\r\n#L3#Thief#l";
            if (jobs[4].length > 0)
                text += "\r\n#L4#Pirate#l";
            cm.sendSimple(text);
        }
    } else if (status == 2) {
        jobSelArray = jobs[FTW];
        if (jobSelArray.length == 0) {
            cm.sendOk("I don't think you've ever been this job before..");
            cm.gainItem(2022065, 1, false);
            cm.dispose();
        } else {
            var text = "Please select a sub job !#b";
            for (var x = 0; x < jobSelArray.length; x++)
                text += "\r\n#L"+x+"#"+cm.getJobById(jobSelArray[x])+"#l";
            cm.sendSimple(text);
        }
    } else if (status == 3) {
        cm.getPlayer().setJob(jobSelArray[FTW]);
        cm.getPlayer().removeJobSkills();
        cm.dispose();
    }
}