# recommend-system
user-user CF, item-item CF and content base system

It's a course project about recommendation system, using Lenskit library which is a professional ppen-source tools for Recommender Systems.
The original dataset contains explicit user ratings raging from 1 to 10, from 275k users and 271k books. The traning data has 606k ratings from 56k users.
What we do is to predict the rating for 4 items for 100 user, which means total 400 predictions. Every need-predict data has been marked by '55'.

By using the recommend system, type "gradlew predict -PuserId=(whatever number) -PitemIds=(whatever numbers seperated by comma)" then it will output a csv file
 which includes these 400 predictions scores.
