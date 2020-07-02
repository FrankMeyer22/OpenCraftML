package FilesManagement;

import Algorithm.Displayer;

//compl�ments de doc : PhP,2016/01/26Ma.
/*
	TODO 2016/01/27-Me, PhP - PB DES ERREURS / EXCEPTION (undef top etc).
	On peut toujours clasher le prog comme erreur interne...
	Mais l'ennui est... que �a fait aussi planter l'Interface Graphique,
	et qu'il est dommage de l'arr�ter alors qu'elle fait d'autres choses...

*/
/**
	* Classe utilitaire pour retenir le TOP N de scores (ou de similarit�s,...)
	* d'une s�rie d'ID (lignes ou colonnes) qui arrivent � vol�e ;
	*
	* sert pour avoir les lignes/colonnes les plus denses,
	* et pour le calcul des K-plus-proches-voisins et des TOP N (ex: Precision) ;
	*
	* ATTENTION, quelques pr�cautions � prendre(*).
	*
	*<P>(*) PRECAUTIONS.
	*<DIR>
	*	<P>ADD's - REDONDANCES -
	*		ATTENTION on suppose que chaque ID n'est vu qu'une fois :
	*		si un ID est vu plusieurs fois il pourra �tre m�moris�
	*		plusieurs fois dans la liste...
	*	<P>ADD's - ARGS VALIDES -
	*		le prog ne contr�le pas la validit� des arguments du ADD : cha�ne vide, null...
	*		la chose devra �tre garantie en amont... Ce choix est fait pour des raisons pratiques :
	*		minimiser la redondance des op�rations de contr�le, ne pas jeter Exception, etc...
	*	<P>COMPLETUDE DES TOP-K -
	*		Il faut prendre garde que les Top-K soient complets en fin d'op�ration ;
	*		c'est � dire, au del� des probl�mes d'ID's vides etc (pb des args du Add) :
	*		que toutes les valeurs associ�es � un ID soient remplac�es,
	*		�tant donn� qu'au d�part, les Top-K sont ind�finis (vides ou similairement),
	*		et que seuls les Add's sauront les remplir...
	*		Mais il ne suffit pas pour cela que le nombre d'observations (Add's)
	*		soit sup�rieur ou �gal � K : il faut aussi que les Add's v�hiculent
	*		suffisamment de propri�t�s (au moins K donc) pour remplir les Top-K...
	*		Ainsi par exemple : 1000 Add(PROPID,value,...)'s mais de la m�me propri�t� (PROPID)
	*		ne rempliront jamais la table des Top-K	(sauf si K=1), mais une seule case...
	*		En pratique, les donn�es usuellement fournies ne devraient pas poser de probl�me,
	*		toutefois il faut en toute rigueur tenir compte de la chose ;
	*		un compteur est impl�ment� en ce but et permet de savoir.
	*	    <BR><BR>
	*       ATTENTION ! Pour avoir les TopK tous d�finis, il faut suffisamment de variables
	*       pour remplir les K cellules... mais comme ce ne sont pas les m�mes variables (Props)
	*       qui viendront en TopK pour tous les Items (lignes, ou sym�triquement : Props,Cols),
	*       cela signifie que m�me pour Seulement K TopK's, m�me peu grand, m�me partiel sur la ligne (sym: col),
	*       il faut potentiellement calculer TOUS les scores de la LogMatrice...
	*   <P>INITIALISATION ET REMPLACEMENTS -
	*   	La <U>Table des Top-Scores</U> est initialis�e � -Infinity (la Table des Top-Value's aussi, du reste).
	*   	<U>S'il y reste des -Infinity � la fin, c'est que les cellules de ces positions n'ont
	*          jamais �t� remplac�es, faute de candidats, m�me tr�s faibles</U>...
	*   	D'abord parce que les candidats sont tous d�finis en principe,
	*   	et parce que de toutes fa�ons, pour entrer dans les Top-K,
	*   	il faut �tre strictement sup�rieur au min des Top-K,
	*   	or il n'y a aucun nombre plus petit que Infinity,
	*   	donc m�me si des candidats (scores) �taient -Infinity, ils n'entreraient pas (...).
	*
	*</DIR>
	*
	* ATTENTION - prendre garde que le nombre d'observations soit sup�rieur ou �gal � N dans Top N.
	*
	*<P>PRINCIPE :<DIR>
	*	On a un ensemble total dont on veut les K meilleurs (ex: K=ordre de la pr�cision) ;<BR>
	*  les �l�ments de l'ensemble total arrivent au fur et � mesure (� la vol�e) pour candidater au bac :<BR>
	*  on garde en effet (en continu) un "bac" des K meilleurs �l�ments (Top K) ;<BR>
	*  quand le nouvel arrivant est meilleur que le plus mauvais du bac, il prend sa place (et l'autre est �ject�) ;<BR>
	*  � la fin, le bac contient forc�ment les meilleurs �l�ments ;<BR>
	*  pour finir, on trie le bac, et on a les K meilleurs dans l'ordre (d�croissant).</DIR>
	*
	* @author ofmu6285
	*
 */
public class BestIndexFinderInOnePass
{
	//TODO: le cas de cases du Top-K restant vides... (cas de donn�es peu nombreuses)

		/**
		 * La Table[K] des ID's des �l�ments du Top-K.
		 */
	public int[] bestID;

		/**
		 * La Table[K] des Valeurs des �l�ments du Top-K ; ce sont des Scores.
		 */
	public float[] bestScore;

		/**
		 * Comme bestScore, sauf que ce sont des Valeurs R�elles ;
		 * elles n'interviennent que passivement, en restant
		 * parall�lement et continuement associ�es aux Scores.
		 *<P>NB - l'ID associ� au score �tant connu, il est vrai
		 *   qu'on pourrait retrouver la pr�sente valeur associ�e via l'ID, 
		 *   mais cela suppose d'avoir une table � clefs vers ces valeurs - pas forc�ment le cas, 
		 *   ensuite m�me en l'ayant, serait-ce plus "rentable", de la cr�er puis d'y acc�der ???
		 *   Noter qu'il faudrait une table pour chaque El�ment (tq Item) dont on cherche le Top-K : 
		 *   il se peut notamment qu'on travaille directement en Fichier sans ItemSet (etc)...   
		 */
	public float[] bestRealValue=null; //ajout PhP 2016/01/26-ma.

		/**
		 * L'ordre K du Top-K.
		 */
	int size;

		/**
		 * (<I>aux</I>) Position (indice), dans le Top-K, du plus petit �l�ment du Top-K courant.
		 */
	int lowerValueIndexCache;

		/**
		 * (<I>aux</I>) Valeur du plus petit �l�ment du Top-K courant.
		 */
	float lowerValueCache;

		/**
			* Nom affich� pour ID (alias PropID) Vide ; ce cas, qui est une ERREUR,
			* se pr�sente lorsque les Add's n'ont pas apport� toutes les occurrences
			* n�cessaires et suffisamment vari�es en ID's pour combler les Top-K,
			* et un ID vide est donc le signe d'incompl�tude des ID's...
			*<P>Noter que les Add's ne peuvent pas contr�ler la chose,
			* puisqu'il ne s'agit pas de Add's avec des ID's ind�finis,
			* mais de Add's directement absents...
			*/
	String mEmptyID="?";//ou ""

		/**	Nombre de Top-K d�finis (�a doit valoir K).
		 *<P>Nombre de Cellules(*) remplac�es dans les tables du Top-K (au fil des Add's) ;
		 * <U>il doit valoir K</U> ; sinon, cela signifie que les Add's n'ont pas apport�
		 * une assez grande vari�t� d'ID's (alias PropID's), ce qui signifie,
		 * qu'il n'y a pas assez d'observations, et/ou, plus insidieusement,
		 * que les Propri�t�s ne sont pas toutes d�finies (cf doc de classe).
		 *<P>*Nb Cells - ne tient �videmment pas compte du nombre de tables[K]...
		 */
	int mNbTopsDefined=0; //ajout PhP 2016/01/27-me
	//
	public int getNbTopsDefined(){return mNbTopsDefined;}
	public int getNbTopsUnDefined(){return (size-mNbTopsDefined);}
	public boolean isTopKDefined(){return (mNbTopsDefined==size);}

		/**
		 * Nombre de Adds (tout compris, qu'ils soient ou non mis dans les Top-K).
		 */
	int mNbAddsTotal=0;

	public int getNbAddsTotal(){return mNbAddsTotal;}

	/**
	 * Constructeur avec en arg le nombre K de Top-K � retenir (op�ration 1 sur 3, unique) ;
	 * au sortir du Constructeur, toutes les tables et autres variables sont initialis�es,
	 * en sorte que le module est pr�t � recevoir les Add's et ce qui s'ensuit.
	 * @param size  Le K du Top-K.
	 */
	public BestIndexFinderInOnePass(int size)
	{
		this.bestID=new int[size];
		this.bestScore=new float[size];
		this.bestRealValue=new float[size];//ajout PhP 2016/01/26-ma.

		this.size=size;

		for (int i=0;i<size;i++)
		{
			this.bestID[i]=-1; //modif PhP 2016/01/27-Me (empty value)
			this.bestScore[i]=Float.NEGATIVE_INFINITY;
			this.bestRealValue[i]=Float.NEGATIVE_INFINITY;//ajout PhP 2016/01/26-ma.
		}
		this.lowerValueIndexCache=0;
		this.lowerValueCache=Float.NEGATIVE_INFINITY;

		mNbTopsDefined=0; //ajout PhP 2016/01/27-me
	}

		/** (<I>aux</I>) cherche le plus petit �l�ment du Top-K (son indice).
		 * @return indice du plus petit �l�ment du Top-K courant ; nota :
		 *         comparaison avec candidat inf�rieur strict � min courant,
		 *         donc, notamment, en cas d'�galit�, c'est statu quo...
		 *         Pour cette raison, notamment, le -Infinity d'initialisation
		 *         ne saurait �tre remplac�, car les candidats sont tous d�finis
		 *         (et s'il l'�tait quand-m�me (inf ou �gal), ce serait par lui-m�me)...
		 */
	public int getLowerScoreIndex() {
		int index=0;
		float lowerValue=this.bestScore[0];
		for (int i=1;i<size;i++) {
			if (this.bestScore[i]<lowerValue) {
				lowerValue=this.bestScore[i];
				index=i;
			}
		}
		return index;
	}

		/**  (<I>aux</I>) Mise � jour de la position du plus petit �l�ment du Top-K courant.
			 */
	public void updateCache() {
		lowerValueIndexCache=getLowerScoreIndex();
		lowerValueCache=this.bestScore[lowerValueIndexCache];
	}

	/**
	 * Addition d'un �l�ment ; on donne son ID et sa Valeur (op�ration 2 sur 3, r�p�titive) ;
	 * apr�s l'addition, le Top-K restera le Top-K, car l'�l�ment n'aura �t� accept�
	 * dans le Top-K que s'il �tait meilleur que le plus mauvais du Top-K avant le add.
	 * @param id     ID de l'�l�ment candidat au Top-K.
	 * @param value  Valeur de l'�l�ment.
	 */
	public void add(int id, float value)
	{
		mNbAddsTotal++;

			//ne remplacer que si la Value est PLUS GRANDE STRICT QUE MIN TOPK - modif PhP, 2016/01/29-Ve
				//notamment �vite des d�placements a priori non utiles (=>garde plut�t les prems colonnes).
				//en g�n�ral dans un tri, on ne prend que les meilleurs stricts!!!
				//il est vrai que sur des valeurs aussi diversifi�es que les Scores, 
				//cela devrait changer - le plus souvent - peu de choses...
				//
		  if (value<=lowerValueCache) //candidat: �tre plus grand strictement que le plus petit (strict) des Top-K.
		//if (value<lowerValueCache) //OLD MODE - modif PhP, 2016/01/29-Ve
		{
			// nothing
		} else {
			this.bestID[lowerValueIndexCache]=id;
			this.bestScore[lowerValueIndexCache]=value;
			updateCache();
		}

		/**
		for (int i=0;i<size;i++) {
			if (bestScore[i]<value) {
				this.bestID[i]=id;
				this.bestScore[i]=value;
				break;
			}
		}
		**/
	}
	public void add(int id, float score, float value_real)
	{
		mNbAddsTotal++;

			//ne remplacer que si la Value est PLUS GRANDE STRICT QUE MIN TOPK - modif PhP, 2016/01/29-Ve
				//notamment �vite des d�placements a priori non utiles (=>garde plut�t les prems colonnes).
				//en g�n�ral dans un tri, on ne prend que les meilleurs stricts!!!
				//il est vrai que sur des valeurs aussi diversifi�es que les Scores, 
				//cela devrait changer - le plus souvent - peu de choses...
				//
		  if (score<=lowerValueCache) //candidat: �tre plus grand strictement que le plus petit (strict) des Top-K.
		//if (score<lowerValueCache) //OLD MODE - modif PhP, 2016/01/29-Ve
		{
			// nothing
		} else {
			//REPLACE!!!
			//mais d'abord (et pas apr�s) un petit test de remplissage...
			if(this.bestScore[lowerValueIndexCache]==Float.NEGATIVE_INFINITY)
				{mNbTopsDefined++;} //�a pourra �tre <K mais pas >K...
			this.bestID[lowerValueIndexCache]=id;
			this.bestScore[lowerValueIndexCache]=score;
			this.bestRealValue[lowerValueIndexCache]=value_real;//ajout PhP 2016/01/26-ma.
			updateCache();
		}

		/**
		for (int i=0;i<size;i++) {
			if (bestScore[i]<value) {
				this.bestID[i]=id;
				this.bestScore[i]=value;
				break;
			}
		}
		**/
	}

		/** Printe le Top-K.*/
	public void print() {
		for (int i=0;i<size;i++) {
			Displayer.displayText("'"+bestID[i]+"'"+"="+bestScore[i]+"\t");
			//System.out.print(bestID[i]+", "+bestScore[i]+"\t");
		}
		Displayer.displayText("");
	}

	public void print2() //ajout PhP 2016/01/26-ma. =>bestRealValue
	{
		Displayer.displayText("bestID"+"\t"+"bestScore"+"\t"+"bestRealValue"+"\n");

		for (int i=0;i<size;i++)
		{
			Displayer.displayText(bestID[i]+"\t"+bestScore[i]+"\t"+bestRealValue[i]+"\n");
		}
		Displayer.displayText("");
	}

	/**
	 * Tri des Top-K par ordre d�croissant (op�ration 3 sur 3, unique) ;
	 * en effet, les add's entretiennent le Top-K en continu,
	 *   mais pas dans l'ordre.
	 *<P>NB - L'algo de tri, peu performant, fait l'affaire pour les Top-K,
	 *   dans la mesure o� K est petit et que le tri n'est pas trop fr�quent
	 *   (recherches successives de Top-K's).
	 */
	public void bubbleSort()
	{
		boolean cont=true;

		while (cont)
		{
			cont=false;

			for (int i=1;i<this.bestScore.length;i++)
			{
				if (this.bestScore[i-1]<this.bestScore[i])
				{
					// SWAP
					float tmp;
					float tmp_real;
					int tmpID;

					tmp=this.bestScore[i-1];
					tmpID=this.bestID[i-1];
					tmp_real=this.bestRealValue[i-1];//ajout PhP 2016/01/26-ma.

					this.bestScore[i-1]=this.bestScore[i];
					this.bestID[i-1]=this.bestID[i];
					this.bestRealValue[i-1]=this.bestRealValue[i];//ajout PhP 2016/01/26-ma.

					this.bestID[i]=tmpID;
					this.bestScore[i]=tmp;
					this.bestRealValue[i]=tmp_real;//ajout PhP 2016/01/26-ma.

					cont=true;
				}
			}
		}
	}


	/** Exemples.*/
	public static void main(String[] p) {

		System.out.println("Best 10 on 0-99");
		BestIndexFinderInOnePass myBest=new BestIndexFinderInOnePass(10);

		for (int i=0; i<100;i++) {
			myBest.add(i+i, i);
		}

		myBest.print();
		System.out.println("SORTING....");

		myBest.bubbleSort();

		myBest.print();
		System.out.println();


		System.out.println("generating 1000+");
		for (int i=1000; i>0;i--) {
			myBest.add(+i, i);
		}


		myBest.print();
		System.out.println("SORTING....");

		myBest.bubbleSort();
		myBest.print();
		System.out.println();


		System.out.println("generating 100 random(1000), keep 10 best ones");
		myBest = new BestIndexFinderInOnePass(10);

		for (int i=0; i<100;i++) {
			int random = (int) (Math.random()*1000);
			myBest.add(i+random, random);
		}

		myBest.print();

		System.out.println("SORTING....");

		myBest.bubbleSort();

		myBest.print();
	}//main
}//class

