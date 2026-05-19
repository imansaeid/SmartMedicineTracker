from django.apps import AppConfig


class ApiConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'api'

    def ready(self):
        import os
        if os.environ.get('RUN_MAIN') == 'true':
            from . import scheduler
            scheduler.start()