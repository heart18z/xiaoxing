"""Scoped semantic conflict and immediate assignment notification backend release."""
import importlib.util, os, sys
from pathlib import Path
spec=importlib.util.spec_from_file_location('base_deploy',Path(__file__).with_name('deploy-applications134.py'))
base=importlib.util.module_from_spec(spec);spec.loader.exec_module(base)
base.TAG='schedule138-20261015'
base.RELEASE=base.ROOT/'releases'/base.TAG
base.BACKUP=base.ROOT/'backups'/('pre-'+base.TAG)
base.IMAGE='aimessage-backend:'+base.TAG
names=['SmartScheduleService','SmartScheduleService$Reviewed','ConflictPrivacy','SmartReminderService',
       'SmartReminderService$EventActionResult','SmartReminderService$FeedbackResult','SmartReminderService$PreparedChat',
       'SmartReminderService$RecipientResolveResult','SmartReminderService$ReplyFieldStreamer','SmartReminderService$ResolveResult']
base.ENTRIES={'BOOT-INF/classes/org/springblade/modules/smartreminder/service/'+name+'.class' for name in names}
if __name__=='__main__':
    os.umask(0o077)
    assert base.run(['hostname']).decode().strip()=='iZuf6dadmqz37mvl5bobysZ'
    {'prepare':base.prepare,'build':base.build,'cutover':base.cutover,'rollback':base.rollback}[sys.argv[1]]()
