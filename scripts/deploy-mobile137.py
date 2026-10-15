"""Scoped backend release: alarm planning and an additional speech protocol."""
import importlib.util, os, sys
from pathlib import Path
spec=importlib.util.spec_from_file_location('base_deploy',Path(__file__).with_name('deploy-applications134.py'))
base=importlib.util.module_from_spec(spec);spec.loader.exec_module(base)
base.TAG='mobile137-20261015'
base.RELEASE=base.ROOT/'releases'/base.TAG
base.BACKUP=base.ROOT/'backups'/('pre-'+base.TAG)
base.IMAGE='aimessage-backend:'+base.TAG
names=['service/AiConfigService','service/AiConfigService$AiRuntimeConfig','service/SpeechService',
       'support/AlarmPlanning','support/DashScopeSpeech','service/SmartReminderService',
       'service/SmartReminderService$EventActionResult','service/SmartReminderService$FeedbackResult',
       'service/SmartReminderService$PreparedChat','service/SmartReminderService$RecipientResolveResult',
       'service/SmartReminderService$ReplyFieldStreamer','service/SmartReminderService$ResolveResult']
base.ENTRIES={'BOOT-INF/classes/org/springblade/modules/smartreminder/'+name+'.class' for name in names}
if __name__=='__main__':
    os.umask(0o077)
    assert base.run(['hostname']).decode().strip()=='iZuf6dadmqz37mvl5bobysZ'
    {'prepare':base.prepare,'build':base.build,'cutover':base.cutover,'rollback':base.rollback}[sys.argv[1]]()
